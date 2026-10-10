package io.ddd4j.ai.extension.chat.autoconfigure;

import io.ddd4j.ai.extension.chat.properties.ChatProperties;
import io.ddd4j.ai.extension.chat.service.ChatService;
import io.ddd4j.ai.extension.chat.service.impl.ChatClientAdapter;
import io.ddd4j.ai.extension.memory.service.MemoryService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 对话自动装配：依赖业务侧模型 starter 提供的 {@link ChatClient.Builder}
 * （Spring AI 标准范式：引入任意 spring-ai-starter-model-* 即有）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@AutoConfiguration
@ConditionalOnClass(ChatClient.class)
@ConditionalOnBean(ChatClient.Builder.class)
@ConditionalOnProperty(name = "ddd4j.ai.chat.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ChatProperties.class)
public class ChatAutoConfiguration {

    /**
     * 构造 ChatAutoConfiguration 自动装配类（由 Spring 容器实例化）。
     */
    public ChatAutoConfiguration() {
    }

    /**
     * 构建对话服务：用业务侧 {@code ChatClient.Builder} 构建客户端，
     * 组合可选记忆服务与默认系统提示词包装为 {@link ChatClientAdapter}。
     *
     * @param chatClientBuilder Spring AI 对话客户端构建器（由模型 starter 提供）
     * @param properties        对话配置（默认系统提示词等）
     * @param memoryService     记忆服务延迟提供器；缺失时仅支持单轮对话
     * @return {@link ChatService} 实现
     */
    @Bean
    @ConditionalOnMissingBean(ChatService.class)
    public ChatService chatService(ChatClient.Builder chatClientBuilder, ChatProperties properties,
                                   ObjectProvider<MemoryService> memoryService) {
        return new ChatClientAdapter(chatClientBuilder.build(),
                memoryService.getIfAvailable(), properties.getDefaultSystemPrompt());
    }
}
