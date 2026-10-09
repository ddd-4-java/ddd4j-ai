package io.ddd4j.ai.extension.embedding.autoconfigure;

import io.ddd4j.ai.extension.embedding.properties.EmbeddingProperties;
import io.ddd4j.ai.extension.embedding.service.EmbeddingService;
import io.ddd4j.ai.extension.embedding.service.impl.EmbeddingModelAdapter;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 向量嵌入自动装配：依赖业务侧模型 starter 提供的 {@link EmbeddingModel}。
 *
 * @author ddd4j
 */
@AutoConfiguration(afterName = {
        "org.springframework.ai.model.ollama.autoconfigure.OllamaEmbeddingAutoConfiguration",
        "org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration"})
@ConditionalOnClass(EmbeddingModel.class)
@ConditionalOnBean(EmbeddingModel.class)
@ConditionalOnProperty(name = "ddd4j.ai.embedding.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(EmbeddingProperties.class)
public class EmbeddingAutoConfiguration {

    /**
     * 默认构造器（自动装配类无状态）。
     */
    public EmbeddingAutoConfiguration() {
    }

    /**
     * 装配向量嵌入服务（适配业务侧 {@link EmbeddingModel}，按 batchSize 分片）。
     *
     * @param embeddingModel 业务侧提供的嵌入模型
     * @param properties     嵌入配置属性
     * @return 向量嵌入服务 Bean
     */
    @Bean
    @ConditionalOnMissingBean(EmbeddingService.class)
    public EmbeddingService embeddingService(EmbeddingModel embeddingModel, EmbeddingProperties properties) {
        return new EmbeddingModelAdapter(embeddingModel, properties.getBatchSize());
    }
}
