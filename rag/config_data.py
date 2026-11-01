import os

md5_path = "./md5.text"

# Chroma
collection_name = "rag"
persist_directory = "./chroma_db"

# 会话历史目录（与 file_history_store 默认一致，可单独配置）
chat_history_dir = "./chat_history"

# spliter
chunk_size = 1000
chunk_overlap = 100
separators = ["\n\n", "\n", ".", "!", "?", "。", "！", "？", " ", ""]
max_split_char_number = 1000        # 文本分割的阈值

#
similarity_threshold = 1            # 检索返回匹配的文档数量

embedding_model_name = "qwen3-embedding:8b"
chat_model_name = "qwen3:8b"

# 溯源助手回答更贴档案、少跑题（0~1，越低越保守）
assistant_temperature = 0.25

# 按溯源码检索无命中时返回（不调用 LLM，避免编造）
empty_retrieval_message = "当前产品的详细溯源档案正在同步中，请稍后再试。"

# 内部同步接口鉴权：环境变量 RAG_SYNC_TOKEN 非空则要求在请求头携带 X-Sync-Token
sync_api_token = os.environ.get("RAG_SYNC_TOKEN", "").strip()

session_config = {
        "configurable": {
            "session_id": "user_001",
        }
    }
