package io.ddd4j.ai.cmpt.rag.autoconfigure;

import io.ddd4j.ai.cmpt.chat.service.ChatService;
import io.ddd4j.ai.cmpt.rag.properties.RagProperties;
import io.ddd4j.ai.cmpt.rag.service.RagService;
import io.ddd4j.ai.cmpt.rag.service.Reranker;
import io.ddd4j.ai.cmpt.rag.service.impl.RagPipeline;
import io.ddd4j.ai.cmpt.rag.service.NoopReranker;
import io.ddd4j.ai.cmpt.vectordb.service.VectorDbService;
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

    @Bean
    @ConditionalOnMissingBean
    public Reranker reranker() {
        return new NoopReranker();
    }

    @Bean
    @ConditionalOnMissingBean(RagService.class)
    public RagService ragService(ChatService chatService, VectorDbService vectorDbService,
                                 Reranker reranker, RagProperties properties) {
        return new RagPipeline(chatService, vectorDbService, reranker, properties);
    }
}
