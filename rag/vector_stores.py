from langchain_chroma import Chroma
import config_data as config


class VectorStoreService(object):
    def __init__(self, embedding):
        """
        :param embedding: 嵌入模型的传入
        """
        self.embedding = embedding
        self.vector_store = Chroma(
            collection_name=config.collection_name,
            embedding_function=self.embedding,
            persist_directory=config.persist_directory,
        )

    def get_retriever(self, product_code: str | None = None):
        """返回向量检索器。传入 product_code 时仅检索该溯源码对应元数据片段。"""
        kwargs: dict = {"k": config.similarity_threshold}
        if product_code:
            kwargs["filter"] = {"product_code": product_code}
        return self.vector_store.as_retriever(search_kwargs=kwargs)

