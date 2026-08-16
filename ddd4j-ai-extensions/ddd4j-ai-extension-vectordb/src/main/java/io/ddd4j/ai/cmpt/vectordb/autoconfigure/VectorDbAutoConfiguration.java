package io.ddd4j.ai.cmpt.vectordb.autoconfigure;

import io.ddd4j.ai.cmpt.vectordb.properties.VectorDbProperties;
import io.ddd4j.ai.cmpt.vectordb.service.VectorDbService;
import io.ddd4j.ai.cmpt.vectordb.service.impl.VectorStoreAdapter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 向量数据库自动装配：依赖业务侧向量库 starter 提供的 {@link VectorStore}。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@AutoConfiguration(afterName = {
        "org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.milvus.autoconfigure.MilvusVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.redis.autoconfigure.RedisVectorStoreAutoConfiguration"})
@ConditionalOnClass(VectorStore.class)
@ConditionalOnBean(VectorStore.class)
@ConditionalOnProperty(name = "ddd4j.ai.vectordb.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(VectorDbProperties.class)
public class VectorDbAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(VectorDbService.class)
    public VectorDbService vectorDbService(VectorStore vectorStore, VectorDbProperties properties) {
        return new VectorStoreAdapter(vectorStore, properties.getDefaultTopK());
    }
}
