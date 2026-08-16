package io.ddd4j.ai.cmpt.embedding.service.impl;

import io.ddd4j.ai.cmpt.embedding.service.EmbeddingService;
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

    public EmbeddingModelAdapter(EmbeddingModel embeddingModel, int batchSize) {
        this.embeddingModel = Objects.requireNonNull(embeddingModel, "embeddingModel");
        this.batchSize = Math.max(1, batchSize);
    }

    @Override
    public float[] embed(String text) {
        return embeddingModel.embed(text);
    }

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

    @Override
    public int dimensions() {
        return embeddingModel.dimensions();
    }
}
