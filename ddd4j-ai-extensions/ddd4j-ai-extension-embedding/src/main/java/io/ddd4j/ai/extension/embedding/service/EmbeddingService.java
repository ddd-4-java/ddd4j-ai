package io.ddd4j.ai.extension.embedding.service;

import java.util.List;

/**
 * 向量嵌入端口：单条与批量文本向量化。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public interface EmbeddingService {

    /**
     * 单文本嵌入。
     *
     * @param text 输入文本
     * @return 嵌入向量
     */
    float[] embed(String text);

    /**
     * 批量嵌入：实现负责按 batchSize 分片调用底层模型，返回顺序与输入一致。
     *
     * @param texts 输入文本列表
     * @return 嵌入向量列表
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * 嵌入维度（由底层模型决定）。
     *
     * @return 维度
     */
    int dimensions();
}
