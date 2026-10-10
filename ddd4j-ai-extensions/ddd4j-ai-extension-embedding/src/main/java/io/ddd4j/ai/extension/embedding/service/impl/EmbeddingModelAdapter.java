package io.ddd4j.ai.extension.embedding.service.impl;

import io.ddd4j.ai.extension.embedding.service.EmbeddingService;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 向量嵌入适配器：委托 Spring AI {@link EmbeddingModel}；
 * 批量请求按 batchSize 分片，避免超出供应商单批上限。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class EmbeddingModelAdapter implements EmbeddingService {

    private final EmbeddingModel embeddingModel;

    private final int batchSize;

    /**
     * 构造嵌入适配器。
     *
     * @param embeddingModel 底层嵌入模型（非空）
     * @param batchSize      批量分片大小；小于 1 时按 1 处理
     * @throws NullPointerException 当 {@code embeddingModel} 为 {@code null} 时
     */
    public EmbeddingModelAdapter(EmbeddingModel embeddingModel, int batchSize) {
        this.embeddingModel = Objects.requireNonNull(embeddingModel, "embeddingModel");
        this.batchSize = Math.max(1, batchSize);
    }

    /**
     * 单条文本嵌入。
     *
     * @param text 待嵌入文本
     * @return 向量
     */
    @Override
    public float[] embed(String text) {
        return embeddingModel.embed(text);
    }

    /**
     * 批量文本嵌入：按 {@code batchSize} 分片调用底层模型，规避供应商单批上限。
     *
     * @param texts 待嵌入文本列表（非空）
     * @return 与入参顺序一致的向量列表
     * @throws NullPointerException 当 {@code texts} 为 {@code null} 时
     */
    @Override
    public List<float[]> embedBatch(List<String> texts) {
        Objects.requireNonNull(texts, "texts");
        List<float[]> result = new ArrayList<>(texts.size());
        for (int from = 0; from < texts.size(); from += batchSize) {
            List<String> slice = texts.subList(from, Math.min(from + batchSize, texts.size()));
            result.addAll(embeddingModel.embed(slice));
        }
        return result;
    }

    /**
     * 返回底层模型的向量维度。
     *
     * @return 向量维度
     */
    @Override
    public int dimensions() {
        return embeddingModel.dimensions();
    }
}
