package io.ddd4j.ai.cmpt.chat.autoconfigure;

import io.ddd4j.ai.cmpt.chat.properties.ChatProperties;
import io.ddd4j.ai.cmpt.chat.service.ChatService;
import io.ddd4j.ai.cmpt.chat.service.impl.ChatClientAdapter;
import io.ddd4j.ai.cmpt.memory.service.MemoryService;
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
@AutoConfiguration(afterName = "org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration")
@ConditionalOnClass(ChatClient.class)
@ConditionalOnBean(ChatClient.Builder.class)
@ConditionalOnProperty(name = "ddd4j.ai.chat.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ChatProperties.class)
public class ChatAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ChatService.class)
    public ChatService chatService(ChatClient.Builder chatClientBuilder, ChatProperties properties,
                                   ObjectProvider<MemoryService> memoryService) {
        return new ChatClientAdapter(chatClientBuilder.build(),
                memoryService.getIfAvailable(), properties.getDefaultSystemPrompt());
    }
}
