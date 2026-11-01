"""
溯源集成用 FastAPI 服务：POST /chat 、POST /admin/knowledge/sync
启动：uvicorn app_api:app --host 0.0.0.0 --port 8000
"""
import logging
from typing import Any

from fastapi import FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

import config_data as config
from knowledge_base import KnowledgeBaseService
from rag import RagService

app = FastAPI(title="农产品溯源 RAG 服务")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

_rag: RagService | None = None
_kb: KnowledgeBaseService | None = None


def get_rag() -> RagService:
    global _rag
    if _rag is None:
        _rag = RagService()
    return _rag


def get_kb() -> KnowledgeBaseService:
    global _kb
    if _kb is None:
        _kb = KnowledgeBaseService()
    return _kb


class ChatRequest(BaseModel):
    product_code: str = Field(..., description="当前页面溯源码")
    question: str = Field(..., description="用户问题")
    session_id: str = Field(..., description="会话标识，建议含溯源码与时间戳")
    trace_context: str | None = Field(
        default=None,
        description="后端下发的全链路溯源 JSON（与扫码查询一致），向量库无文档时仍可依此作答",
    )


class ChatResponse(BaseModel):
    answer: str


def _is_identity_meta_question(text: str) -> bool:
    """用户只问「你是谁」等元问题时，不走 LLM，避免被档案 JSON 带偏成人设/数据记录。"""
    t = (text or "").strip().lower()
    if not t or len(t) > 48:
        return False
    needles = (
        "你是谁",
        "你是什么",
        "你叫什么",
        "你哪位",
        "自我介绍",
        "什么模型",
        "哪个模型",
        "你是gpt",
        "你是chatgpt",
        "通义",
        "claude",
        "ollama",
        "你是ai",
        "你是机器人",
        "你是人吗",
        "你是助手吗",
        "你的身份",
    )
    return any(n in t for n in needles)


_IDENTITY_FIXED_REPLY = (
    "您好！我是 **AI 溯源助手**，专门为您解答当前追溯码的官方档案信息。"
    "我只根据系统里备案的官方档案作答，不会使用其他追溯码或外部常识来冒充本批次信息。"
)


class SyncRequest(BaseModel):
    product_code: str
    documents: list[str]


class SyncResponse(BaseModel):
    detail: str


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/chat", response_model=ChatResponse)
def chat(body: ChatRequest) -> ChatResponse:
    pc = body.product_code.strip()
    if not pc:
        raise HTTPException(status_code=400, detail="product_code 不能为空")
    sid = body.session_id.strip()
    if not sid:
        raise HTTPException(status_code=400, detail="session_id 不能为空")

    cfg: dict[str, Any] = {
        "session_id": sid,
        "product_code": pc,
    }
    if body.trace_context and body.trace_context.strip():
        cfg["trace_context"] = body.trace_context.strip()
    run_config: dict[str, Any] = {"configurable": cfg}

    q = body.question.strip()
    if _is_identity_meta_question(q):
        return ChatResponse(answer=_IDENTITY_FIXED_REPLY)

    try:
        answer = get_rag().chain.invoke({"input": q}, run_config)
        return ChatResponse(answer=str(answer))
    except HTTPException:
        raise
    except Exception as e:
        logging.exception("RAG /chat 执行失败")
        hint = (
            f"{type(e).__name__}: {e}。"
            "请确认：① 本机已执行 `ollama serve`；② 已 `ollama pull` 拉取 "
            f"嵌入模型 `{config.embedding_model_name}` 与对话模型 `{config.chat_model_name}`；"
            "③ `./chroma_db` 目录可读写。"
        )
        raise HTTPException(status_code=503, detail=hint) from e


@app.post("/admin/knowledge/sync", response_model=SyncResponse)
def knowledge_sync(
    body: SyncRequest,
    x_sync_token: str | None = Header(default=None, alias="X-Sync-Token"),
) -> SyncResponse:
    if config.sync_api_token and x_sync_token != config.sync_api_token:
        raise HTTPException(status_code=401, detail="同步接口未授权")

    detail = get_kb().sync_product_documents(body.product_code, body.documents)
    return SyncResponse(detail=detail)
