"""
知识库
"""
import os
import config_data as config
import hashlib
from langchain_chroma import Chroma
from langchain_ollama import OllamaEmbeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter
from datetime import datetime


def check_md5(md5_str: str):
    """检查传入的md5字符串是否已经被处理过了
        return False(md5未处理过)  True(已经处理过，已有记录）
    """
    if not os.path.exists(config.md5_path):
        # if进入表示文件不存在，那肯定没有处理过这个md5了
        open(config.md5_path, 'w', encoding='utf-8').close()
        return False
    else:
        for line in open(config.md5_path, 'r', encoding='utf-8').readlines():
            line = line.strip()     # 处理字符串前后的空格和回车
            if line == md5_str:
                return True         # 已处理过

        return False


def save_md5(md5_str: str):
    """将传入的md5字符串，记录到文件内保存"""
    with open(config.md5_path, 'a', encoding="utf-8") as f:
        f.write(md5_str + '\n')


def get_string_md5(input_str: str, encoding='utf-8'):
    """将传入的字符串转换为md5字符串"""

    # 将字符串转换为bytes字节数组
    str_bytes = input_str.encode(encoding=encoding)

    # 创建md5对象
    md5_obj = hashlib.md5()     # 得到md5对象
    md5_obj.update(str_bytes)   # 更新内容（传入即将要转换的字节数组）
    md5_hex = md5_obj.hexdigest()       # 得到md5的十六进制字符串

    return md5_hex


class KnowledgeBaseService(object):
    def __init__(self):
        # 如果文件夹不存在则创建，如果存在则跳过
        os.makedirs(config.persist_directory, exist_ok=True)

        self.chroma = Chroma(
            collection_name=config.collection_name,     # 数据库的表名
            embedding_function=OllamaEmbeddings(model=config.embedding_model_name),
            persist_directory=config.persist_directory,     # 数据库本地存储文件夹
        )     # 向量存储的实例 Chroma向量库对象

        self.spliter = RecursiveCharacterTextSplitter(
            chunk_size=config.chunk_size,       # 分割后的文本段最大长度
            chunk_overlap=config.chunk_overlap,     # 连续文本段之间的字符重叠数量
            separators=config.separators,       # 自然段落划分的符号
            length_function=len,                # 使用Python自带的len函数做长度统计的依据
        )     # 文本分割器的对象

    def upload_by_str(self, data: str, filename, product_code: str | None = None):
        """将传入的字符串，进行向量化，存入向量数据库中。
        若传入 product_code，元数据会带上该字段，便于与溯源问答检索一致；去重键会包含该码。"""
        # 先得到传入字符串的md5值（按批次区分去重）
        pc = (product_code or "").strip()
        md5_hex = get_string_md5(data + "\n" + filename + "\n" + pc)

        if check_md5(md5_hex):
            return "[跳过]内容已经存在知识库中"

        if len(data) > config.max_split_char_number:
            knowledge_chunks: list[str] = self.spliter.split_text(data)
        else:
            knowledge_chunks = [data]

        metadata = {
            "source": filename,
            # 2025-01-01 10:00:00
            "create_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "operator": "小曹",
        }
        if pc:
            metadata["product_code"] = pc

        self.chroma.add_texts(      # 内容就加载到向量库中了
            # iterable -> list \ tuple
            knowledge_chunks,
            metadatas=[dict(metadata) for _ in knowledge_chunks],
        )

        #
        save_md5(md5_hex)
        return "[成功]内容已经成功载入向量库"

    def sync_product_documents(self, product_code: str, documents: list[str]) -> str:
        """按溯源码幂等同步：删除该码下全部旧向量后，对文档列表切分并写入。"""
        product_code = (product_code or "").strip()
        if not product_code:
            return "[失败]product_code 不能为空"

        if not documents:
            try:
                self.chroma.delete(where={"product_code": product_code})
            except TypeError:
                self._delete_by_product_code_fallback(product_code)
            return "[成功]已清空该溯源码知识（无文档）"

        try:
            self.chroma.delete(where={"product_code": product_code})
        except TypeError:
            self._delete_by_product_code_fallback(product_code)

        texts: list[str] = []
        metadatas: list[dict] = []
        base_time = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

        for doc_index, raw in enumerate(documents):
            raw = (raw or "").strip()
            if not raw:
                continue
            if len(raw) > config.max_split_char_number:
                chunks = self.spliter.split_text(raw)
            else:
                chunks = [raw]

            for chunk_index, chunk in enumerate(chunks):
                texts.append(chunk)
                metadatas.append({
                    "product_code": product_code,
                    "source": f"sync_doc_{doc_index}",
                    "chunk_index": chunk_index,
                    "create_time": base_time,
                    "operator": "traceability_sync",
                })

        if not texts:
            return "[失败]documents 无有效文本"

        self.chroma.add_texts(texts=texts, metadatas=metadatas)
        return f"[成功]已同步溯源码 {product_code}，共 {len(texts)} 条向量片段"

    def _delete_by_product_code_fallback(self, product_code: str) -> None:
        """部分环境下 Chroma.delete 不支持 where 时，按 get(where=...) 删除。"""
        try:
            res = self.chroma.get(where={"product_code": product_code}, include=[])
        except TypeError:
            return
        ids = res.get("ids") or []
        if ids:
            self.chroma.delete(ids=ids)