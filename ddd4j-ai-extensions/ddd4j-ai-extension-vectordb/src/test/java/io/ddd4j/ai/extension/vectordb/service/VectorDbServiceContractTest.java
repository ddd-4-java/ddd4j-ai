package io.ddd4j.ai.extension.vectordb.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link VectorDbService} 端口契约测试：以 SimpleVectorStore（内存实现）验证
 * 入库/检索/删除/metadata 过滤语义。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class VectorDbServiceContractTest {

    /** 确定性二维嵌入：[首字符编码, 常量]，余弦/欧氏均可区分。 */
    static class FirstCharEmbeddingModel implements org.springframework.ai.embedding.EmbeddingModel {

        @Override
        public float[] embed(String text) {
            char c = text.isEmpty() ? 'a' : text.charAt(0);
            return new float[]{c, 100.0f};
        }

        @Override
        public float[] embed(Document document) {
            return embed(document.getText());
        }

        @Override
        public org.springframework.ai.embedding.EmbeddingResponse call(
                org.springframework.ai.embedding.EmbeddingRequest request) {
            java.util.List<org.springframework.ai.embedding.Embedding> embeddings = new java.util.ArrayList<>();
            var instructions = request.getInstructions();
            for (int i = 0; i < instructions.size(); i++) {
                embeddings.add(new org.springframework.ai.embedding.Embedding(
                        embed(instructions.get(i)), i));
            }
            return new org.springframework.ai.embedding.EmbeddingResponse(embeddings);
        }
    }

    /** 每个测试独立向量库实例，避免方法间数据残留。 */
    private VectorDbService vectorDb;

    @BeforeEach
    void setUp() {
        vectorDb = new io.ddd4j.ai.extension.vectordb.service.impl.VectorStoreAdapter(
                SimpleVectorStore.builder(new FirstCharEmbeddingModel()).build(), 4);
    }

    @Test
    void addThenSearchReturnsDocuments() {
        vectorDb.add(List.of(
                new Document("apple pie recipe", java.util.Map.of("kind", "food")),
                new Document("banana bread recipe", java.util.Map.of("kind", "food"))));

        // 'a' 与 apple 相似
        List<Document> hits = vectorDb.search("a", 2, null);

        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).getText()).isEqualTo("apple pie recipe");
    }

    @Test
    void searchAppliesMetadataFilter() {
        vectorDb.add(List.of(
                new Document("apple", java.util.Map.of("kind", "fruit")),
                new Document("apricot", java.util.Map.of("kind", "dried"))));

        FilterExpressionBuilder b = new FilterExpressionBuilder();
        Filter.Expression filter = b.eq("kind", "dried").build();

        List<Document> hits = vectorDb.search("a", 4, filter);

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getText()).isEqualTo("apricot");
    }

    @Test
    void deleteRemovesDocuments() {
        Document doc = new Document("to be deleted", java.util.Map.of());
        vectorDb.add(List.of(doc));

        vectorDb.delete(List.of(doc.getId()));

        assertThat(vectorDb.search("to", 4, null)).isEmpty();
    }
}
