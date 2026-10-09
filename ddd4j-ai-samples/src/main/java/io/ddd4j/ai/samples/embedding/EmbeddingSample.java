package io.ddd4j.ai.samples.embedding;

import io.ddd4j.ai.extension.embedding.service.EmbeddingService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * embedding 向量嵌入组件使用示例：单条与批量向量化。
 *
 * @author ddd4j
 */
@Component
public class EmbeddingSample {

    private final EmbeddingService embeddingService;

    /**
     * 构造 EmbeddingSample。
     *
     * @param embeddingService 向量嵌入服务
     */
    public EmbeddingSample(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    /**
     * 单文本向量化（如查询向量化）。
     *
     * @param text 文本内容
     * @return 文本对应的向量
     */
    public float[] embed(String text) {
        return embeddingService.embed(text);
    }

    /**
     * 批量向量化（如知识库文档预处理），自动按 batchSize 分片。
     *
     * @param texts 文本列表
     * @return 批量向量列表
     */
    public List<float[]> embedBatch(List<String> texts) {
        return embeddingService.embedBatch(texts);
    }

    /**
     * 嵌入维度（向量库集合初始化参考）。
     *
     * @return 嵌入维度值
     */
    public int dimensions() {
        return embeddingService.dimensions();
    }
}
