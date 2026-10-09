package io.ddd4j.ai.extension.memory.autoconfigure;

import io.ddd4j.ai.extension.memory.properties.MemoryProperties;
import io.ddd4j.ai.extension.memory.service.MemoryService;
import io.ddd4j.ai.extension.memory.service.impl.WindowMemoryService;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 会话记忆自动装配：容器中存在 {@link ChatMemoryRepository} bean 时复用之
 * （例如业务侧引入官方 chat-memory-repository-jdbc/redis starter 提供的实现），
 * 否则回退到进程内存储。窗口策略统一为 {@link WindowMemoryService}。
 *
 * @author ddd4j
 */
@AutoConfiguration
@ConditionalOnClass(ChatMemoryRepository.class)
@ConditionalOnProperty(name = "ddd4j.ai.memory.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MemoryProperties.class)
public class MemoryAutoConfiguration {

    /**
     * 默认构造器（自动装配类无状态）。
     */
    public MemoryAutoConfiguration() {
    }

    /**
     * 装配会话记忆服务：优先复用容器内 repository，否则回退进程内存储。
     *
     * @param properties 记忆配置属性
     * @param repository 记忆仓储提供器
     * @return 会话记忆服务 Bean
     */
    @Bean
    @ConditionalOnMissingBean(MemoryService.class)
    public MemoryService memoryService(MemoryProperties properties,
                                       ObjectProvider<ChatMemoryRepository> repository) {
        ChatMemoryRepository backing = repository.getIfAvailable(InMemoryChatMemoryRepository::new);
        return new WindowMemoryService(backing, properties.getWindowSize());
    }
}
