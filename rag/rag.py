from langchain_core.documents import Document
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_core.runnables import (
    RunnableBranch,
    RunnableLambda,
    RunnableParallel,
    RunnablePassthrough,
    RunnableWithMessageHistory,
)
from langchain_core.runnables.config import RunnableConfig
from langchain_ollama import ChatOllama, OllamaEmbeddings

import config_data as config
from file_history_store import get_history
from vector_stores import VectorStoreService

# 检索无文档时的内部标记（与真实上下文区分）
NO_RAG_DOCS = "__NO_RAG_DOCUMENTS__"


def print_prompt(prompt):
    print("=" * 20)
    print(prompt.to_string())
    print("=" * 20)

    return prompt


class RagService(object):
    def __init__(self):

        self.vector_service = VectorStoreService(
            embedding=OllamaEmbeddings(model=config.embedding_model_name)
        )

        self.prompt_template = ChatPromptTemplate.from_messages(
            [
                (
                    "system",
                    "你是一个专业的农产品溯源顾问，你的全部知识来源于当前产品的官方溯源档案。你只能依据提供的'参考资料'回答用户问题，不得使用任何外部知识或预训练知识。\n"
                    "当前唯一有效的核验主体为追溯码：{trace_code}。你必须仅依据该追溯码对应的官方档案回答，"
                    "不得扩展到其他追溯码、其他商品或其他独立批次故事。\n\n"
                    "【必须遵守】\n"
                    "1. 事实来源仅限下方「参考资料」中出现的字段与数值；如果参考资料中没有包含用户所问的信息，请如实告知：'根据当前溯源档案，暂时没有找到相关信息。'不要猜测或编造。\n"
                    "2. 参考资料中若同时出现成品批次、生产批次、销售单等编号，均视为与本追溯码同一条核验链路内的信息，可说明其关联；"
                    "不得把它们写成与当前扫描商品无关的另一批货。\n"
                    "2b. 若 JSON 中存在 consumerFacing：用户问「产品名称/叫什么」时，**仅回答** consumerFacing.displayProductName；"
                    "harvestStageRecordName 仅为采收/原料环节在系统内的记录用名，**禁止**与成品名称并列称为两个「产品名」，"
                    "除非用户明确问采收侧或原料记录叫什么。回答中**绝对禁止**出现任何数据库字段名、JSON 键名等开发者用语，"
                    "如 productBatch、prodBatch、productName、saleList、logisticsList、traceCode 等，只能使用自然语言描述。\n"
                    "3. 你的身份只能是「AI 溯源助手」：若用户问「你是谁」等，仅用一两句话说明你是 AI 溯源助手、"
                    "只解读当前追溯码的官方档案；禁止自称供应链里的一条数据记录、一张表、一个批次对象或任何档案实体。"
                    "若用户问底层模型/通义/GPT/Ollama 等：礼貌拒绝透露。\n"
                    "4. 如果用户的问题与产品溯源无关（例如闲聊、询问天气、讲笑话、询问其他产品等），请礼貌地拒绝回答，回复：'抱歉，我只能回答关于当前产品溯源信息的问题。您可以问我种植过程、质检报告、物流状态等。'\n"
                    "5. 使用简体中文，条理清晰；回答消费者时少用原始字段名（如 qualityStatus），改用「质检结论：合格」等自然表述。\n"
                    "参考资料（向量库片段 + 可选官方链路 JSON）：\n{context}",
                ),
                ("system", "以下为同一会话中的近期对话，仅用于理解指代，事实仍以首条系统消息中的参考资料为准。"),
                MessagesPlaceholder("history"),
                ("user", "{input}"),
            ]
        )

        self.chat_model = ChatOllama(
            model=config.chat_model_name,
            temperature=getattr(config, "assistant_temperature", 0.25),
        )

        self.chain = self.__get_chain()

    def __get_chain(self):
        """获取带会话历史、可选按 product_code 过滤的最终执行链。"""

        def format_document(docs: list[Document]) -> str:
            if not docs:
                return NO_RAG_DOCS

            formatted_str = ""
            for doc in docs:
                formatted_str += f"文档片段：{doc.page_content}\n文档元数据：{doc.metadata}\n\n"

            return formatted_str

        def retrieve_docs(value: dict, config: RunnableConfig | None = None) -> list[Document]:
            """RunnableLambda 只会按约定注入名为 config 的参数，不能写成 cfg。"""
            inner = value["input"]
            if isinstance(inner, dict):
                question = inner.get("input", "")
            else:
                question = inner
            cfg = config or {}
            pc = cfg.get("configurable", {}).get("product_code")
            if isinstance(pc, str):
                pc = pc.strip() or None
            else:
                pc = None
            retriever = self.vector_service.get_retriever(product_code=pc)
            docs = retriever.invoke(question)
            if not isinstance(docs, list):
                docs = list(docs)

            # 仅保留元数据中 product_code 与当前追溯码完全一致的向量片段（无元数据的老数据直接丢弃，防串码）
            if pc:
                strict: list[Document] = []
                for d in docs:
                    md = d.metadata or {}
                    if md.get("source") == "tracegrid_open_aggregate":
                        continue
                    mpc = md.get("product_code")
                    if mpc is not None and str(mpc).strip() == str(pc).strip():
                        strict.append(d)
                docs = strict

            raw_tc = (cfg.get("configurable") or {}).get("trace_context")
            tc = raw_tc.strip() if isinstance(raw_tc, str) else ""
            if tc:
                docs = docs + [
                    Document(
                        page_content="【溯源系统官方链路数据（与扫码页一致）】\n" + tc,
                        metadata={"source": "tracegrid_open_aggregate"},
                    )
                ]
            return docs

        def format_for_prompt_template(value: dict) -> dict:
            inner = value["input"]
            raw_tc = value.get("product_code")
            if isinstance(raw_tc, str) and raw_tc.strip():
                trace_disp = raw_tc.strip()
            else:
                trace_disp = "（未提供追溯码）"

            new_value = {
                "input": inner["input"],
                "history": inner["history"],
                "trace_code": trace_disp,
            }
            ctx = value["context"]
            if ctx == NO_RAG_DOCS:
                new_value["context"] = "无相关参考资料"
            else:
                new_value["context"] = ctx
            return new_value

        def short_circuit_gate(value: dict) -> bool:
            pc = value.get("product_code")
            return bool(pc) and value.get("context") == NO_RAG_DOCS

        parallel = RunnableParallel(
            {
                "input": RunnablePassthrough(),
                "context": (
                    RunnableLambda(retrieve_docs)
                    | RunnableLambda(format_document)
                ),
            }
        )

        with_product_code = RunnablePassthrough.assign(
            product_code=RunnableLambda(
                lambda _v, config=None: (config or {}).get("configurable", {}).get(
                    "product_code"
                )
            )
        )

        llm_branch = (
            RunnableLambda(format_for_prompt_template)
            | self.prompt_template
            | print_prompt
            | self.chat_model
            | StrOutputParser()
        )

        branch = RunnableBranch(
            (
                RunnableLambda(short_circuit_gate),
                RunnableLambda(lambda _v: config.empty_retrieval_message),
            ),
            llm_branch,
        )

        chain_core = parallel | with_product_code | branch

        conversation_chain = RunnableWithMessageHistory(
            chain_core,
            get_history,
            input_messages_key="input",
            history_messages_key="history",
        )

        return conversation_chain

