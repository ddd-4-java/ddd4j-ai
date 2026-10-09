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
 * @author ddd4j
 */
@AutoConfiguration(afterName = "org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration")
@ConditionalOnClass(ChatClient.class)
@ConditionalOnBean(ChatClient.Builder.class)
@ConditionalOnProperty(name = "ddd4j.ai.chat.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ChatProperties.class)
public class ChatAutoConfiguration {

    /**
     * 默认构造器（自动装配类无状态）。
     */
    public ChatAutoConfiguration() {
    }

    /**
     * 装配 {@link ChatService}：桥接 Spring AI {@link ChatClient}，可选挂接记忆服务。
     *
     * @param chatClientBuilder Spring AI 对话客户端构建器
     * @param properties        对话配置属性
     * @param memoryService     记忆服务提供器，未注册时不启用对话记忆
     * @return 对话服务 Bean
     */
    @Bean
    @ConditionalOnMissingBean(ChatService.class)
    public ChatService chatService(ChatClient.Builder chatClientBuilder, ChatProperties properties,
                                   ObjectProvider<MemoryService> memoryService) {
        return new ChatClientAdapter(chatClientBuilder.build(),
                memoryService.getIfAvailable(), properties.getDefaultSystemPrompt());
    }
}
