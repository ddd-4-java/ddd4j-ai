package io.ddd4j.ai.samples.vectordb;

import io.ddd4j.ai.cmpt.vectordb.service.VectorDbService;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * vectordb 向量数据库组件使用示例：入库、检索（含 metadata 过滤）与删除。
 * 后端由业务侧引入的 spring-ai-starter-vector-store-*（pgvector/Milvus/Redis 等）决定。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Component
public class VectorDbSample {

    private final VectorDbService vectorDbService;

    public VectorDbSample(VectorDbService vectorDbService) {
        this.vectorDbService = vectorDbService;
    }

    /** 文档入库（向量由向量库关联的嵌入模型生成）。 */
    public void save(List<Document> documents) {
        vectorDbService.add(documents);
    }

    /** 相似度检索。 */
    public List<Document> search(String query, int topK) {
        return vectorDbService.search(query, topK, null);
    }

    /** 带 metadata 过滤的检索（如按租户/来源隔离）。 */
    public List<Document> searchWithTenant(String query, int topK, String tenantId) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        return vectorDbService.search(query, topK, b.eq("tenantId", tenantId).build());
    }

    /** 按文档 id 删除。 */
    public void delete(List<String> documentIds) {
        vectorDbService.delete(documentIds);
    }
}
