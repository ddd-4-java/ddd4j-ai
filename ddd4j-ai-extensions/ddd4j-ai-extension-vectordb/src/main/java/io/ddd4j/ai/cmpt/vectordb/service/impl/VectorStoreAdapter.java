package io.ddd4j.ai.cmpt.vectordb.service.impl;

import io.ddd4j.ai.cmpt.vectordb.service.VectorDbService;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.List;
import java.util.Objects;

/**
 * 通用向量库适配器：委托任意 Spring AI {@link VectorStore} 实现
 * （pgvector / Milvus / Redis / Chroma 等 starter 提供什么就适配什么）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class VectorStoreAdapter implements VectorDbService {

    private final VectorStore vectorStore;

    private final int defaultTopK;

    public VectorStoreAdapter(VectorStore vectorStore, int defaultTopK) {
        this.vectorStore = Objects.requireNonNull(vectorStore, "vectorStore");
        this.defaultTopK = defaultTopK;
    }

    @Override
    public void add(List<Document> documents) {
        vectorStore.add(documents);
    }

    @Override
    public void delete(List<String> ids) {
        vectorStore.delete(ids);
    }

    @Override
    public List<Document> search(String query, int topK, Filter.Expression filter) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(topK > 0 ? topK : defaultTopK);
        if (filter != null) {
            builder.filterExpression(filter);
        }
        return vectorStore.similaritySearch(builder.build());
    }
}
