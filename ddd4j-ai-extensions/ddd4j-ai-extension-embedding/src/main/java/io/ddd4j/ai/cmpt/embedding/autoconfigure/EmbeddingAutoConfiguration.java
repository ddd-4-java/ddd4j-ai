package io.ddd4j.ai.cmpt.embedding.autoconfigure;

import io.ddd4j.ai.cmpt.embedding.properties.EmbeddingProperties;
import io.ddd4j.ai.cmpt.embedding.service.EmbeddingService;
import io.ddd4j.ai.cmpt.embedding.service.impl.EmbeddingModelAdapter;
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
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@AutoConfiguration
@ConditionalOnClass(EmbeddingModel.class)
@ConditionalOnBean(EmbeddingModel.class)
@ConditionalOnProperty(name = "ddd4j.ai.embedding.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(EmbeddingProperties.class)
public class EmbeddingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(EmbeddingService.class)
    public EmbeddingService embeddingService(EmbeddingModel embeddingModel, EmbeddingProperties properties) {
        return new EmbeddingModelAdapter(embeddingModel, properties.getBatchSize());
    }
}
