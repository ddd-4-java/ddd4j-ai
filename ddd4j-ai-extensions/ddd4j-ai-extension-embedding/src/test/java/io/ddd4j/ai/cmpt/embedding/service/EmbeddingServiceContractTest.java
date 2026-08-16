package io.ddd4j.ai.cmpt.embedding.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link EmbeddingService} 端口契约测试：以确定性桩验证单条/批量/维度语义。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class EmbeddingServiceContractTest {

    /** 确定性桩：向量 = 文本哈希填充。 */
    static class HashEmbeddingService implements EmbeddingService {

        @Override
        public float[] embed(String text) {
            return new float[]{text.hashCode(), 1.0f};
        }

        @Override
        public List<float[]> embedBatch(List<String> texts) {
            return texts.stream().map(this::embed).toList();
        }

        @Override
        public int dimensions() {
            return 2;
        }
    }

    private final EmbeddingService embedding = new HashEmbeddingService();

    @Test
    void embedReturnsVectorForText() {
        assertThat(embedding.embed("你好")).containsExactly("你好".hashCode(), 1.0f);
    }

    @Test
    void embedBatchKeepsInputOrder() {
        List<float[]> vectors = embedding.embedBatch(List.of("a", "b", "c"));

        assertThat(vectors).hasSize(3);
        assertThat(vectors.get(0)).containsExactly("a".hashCode(), 1.0f);
        assertThat(vectors.get(2)).containsExactly("c".hashCode(), 1.0f);
    }

    @Test
    void dimensionsMatchesVectorLength() {
        assertThat(embedding.embed("x")).hasSize(embedding.dimensions());
    }
}
