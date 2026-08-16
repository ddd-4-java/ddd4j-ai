package io.ddd4j.ai.cmpt.vectordb.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.List;

/**
 * 向量数据库端口：文档入库、删除与相似度检索。
 * 后端由实现委托的 {@link org.springframework.ai.vectorstore.VectorStore} 决定
 * （业务侧引入任意 spring-ai-starter-vector-store-* 即可）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public interface VectorDbService {

    /**
     * 文档入库（向量由底层 VectorStore 关联的嵌入模型生成）。
     *
     * @param documents 文档列表
     */
    void add(List<Document> documents);

    /**
     * 按 id 删除文档。
     *
     * @param ids 文档 id 列表
     */
    void delete(List<String> ids);

    /**
     * 相似度检索。
     *
     * @param query  查询文本
     * @param topK   返回条数上限
     * @param filter metadata 过滤表达式；null 表示不过滤
     * @return 命中文档（按相似度降序）
     */
    List<Document> search(String query, int topK, Filter.Expression filter);
}
