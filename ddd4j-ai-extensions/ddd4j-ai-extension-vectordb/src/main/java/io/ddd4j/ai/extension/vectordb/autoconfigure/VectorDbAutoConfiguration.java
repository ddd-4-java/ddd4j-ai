package io.ddd4j.ai.extension.vectordb.autoconfigure;

import io.ddd4j.ai.extension.vectordb.properties.VectorDbProperties;
import io.ddd4j.ai.extension.vectordb.service.VectorDbService;
import io.ddd4j.ai.extension.vectordb.service.impl.VectorStoreAdapter;
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
 * @author ddd4j
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

    /**
     * 默认构造器（自动装配类无状态，由 Spring 容器管理）。
     */
    public VectorDbAutoConfiguration() {
    }

    /**
     * 注册向量库服务：容器中已存在 {@link VectorStore} 时，
     * 以默认检索条数包装为 {@link VectorDbService}。
     *
     * @param vectorStore Spring AI 向量存储实现（由业务侧 starter 提供）
     * @param properties  向量数据库组件配置
     * @return 向量库服务实例
     */
    @Bean
    @ConditionalOnMissingBean(VectorDbService.class)
    public VectorDbService vectorDbService(VectorStore vectorStore, VectorDbProperties properties) {
        return new VectorStoreAdapter(vectorStore, properties.getDefaultTopK());
    }
}
