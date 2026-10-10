package io.ddd4j.ai.extension.rag.autoconfigure;

import io.ddd4j.ai.extension.chat.service.ChatService;
import io.ddd4j.ai.extension.rag.properties.RagProperties;
import io.ddd4j.ai.extension.rag.service.RagService;
import io.ddd4j.ai.extension.rag.service.Reranker;
import io.ddd4j.ai.extension.rag.service.impl.RagPipeline;
import io.ddd4j.ai.extension.rag.service.NoopReranker;
import io.ddd4j.ai.extension.vectordb.service.VectorDbService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * RAG 自动装配：chat 与 vectordb 端口就绪时激活；Reranker 默认直通、可被业务覆盖。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@AutoConfiguration
@ConditionalOnClass(RagService.class)
@ConditionalOnBean({ChatService.class, VectorDbService.class})
@ConditionalOnProperty(name = "ddd4j.ai.rag.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(RagProperties.class)
public class RagAutoConfiguration {

    /**
     * 构造 RagAutoConfiguration 自动装配类（由 Spring 容器实例化）。
     */
    public RagAutoConfiguration() {
    }

    /**
     * 注册默认直通重排器（业务侧可同类型 bean 覆盖）。
     *
     * @return 重排器 bean
     */
    @Bean
    @ConditionalOnMissingBean
    public Reranker reranker() {
        return new NoopReranker();
    }

    /**
     * 注册 RAG 管道服务：检索 → 重排 → 模板增强 → 生成。
     *
     * @param chatService    对话端口
     * @param vectorDbService 向量库端口
     * @param reranker       重排器
     * @param properties     RAG 配置
     * @return RAG 服务 bean
     */
    @Bean
    @ConditionalOnMissingBean(RagService.class)
    public RagService ragService(ChatService chatService, VectorDbService vectorDbService,
                                 Reranker reranker, RagProperties properties) {
        return new RagPipeline(chatService, vectorDbService, reranker, properties);
    }
}
