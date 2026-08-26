package io.ddd4j.ai.extension.memory.autoconfigure;

import io.ddd4j.ai.extension.memory.service.MemoryService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import java.util.List;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MemoryAutoConfiguration} 装配测试：默认装配、外部 repository 复用、开关与去重。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class MemoryAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MemoryAutoConfiguration.class));

    @Test
    void registersMemoryServiceByDefault() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(MemoryService.class);
            assertThat(context.getBean(MemoryService.class)).isInstanceOf(
                    io.ddd4j.ai.extension.memory.service.impl.WindowMemoryService.class);
        });
    }

    @Test
    void reusesExternalRepositoryWhenPresent() {
        TrackingRepository external = new TrackingRepository();
        runner.withBean("externalRepository", ChatMemoryRepository.class, () -> external)
                .run(context -> {
                    context.getBean(MemoryService.class)
                            .add("probe", List.of(new org.springframework.ai.chat.messages.UserMessage("hi")));
                    context.getBean(MemoryService.class).get("probe");
                    assertThat(external.used).isTrue();
                });
    }

    @Test
    void backsOffWhenDisabled() {
        runner.withPropertyValues("ddd4j.ai.memory.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MemoryService.class));
    }

    @Test
    void backsOffWhenUserDefinesOwnMemoryService() {
        runner.withBean("customMemory", MemoryService.class,
                        () -> new io.ddd4j.ai.extension.memory.service.impl.WindowMemoryService(
                                new InMemoryChatMemoryRepository(), 5))
                .run(context -> {
                    assertThat(context).hasSingleBean(MemoryService.class);
                    assertThat(context.getBean("customMemory")).isNotNull();
                });
    }

    /** 探针 repository：验证 AutoConfiguration 是否复用了外部实现。 */
    static class TrackingRepository implements ChatMemoryRepository {
        final ChatMemoryRepository delegate = new InMemoryChatMemoryRepository();
        volatile boolean used;

        @Override
        public java.util.List<String> findConversationIds() {
            return delegate.findConversationIds();
        }

        @Override
        public java.util.List<org.springframework.ai.chat.messages.Message> findByConversationId(String conversationId) {
            used = true;
            return delegate.findByConversationId(conversationId);
        }

        @Override
        public void saveAll(String conversationId, java.util.List<org.springframework.ai.chat.messages.Message> messages) {
            used = true;
            delegate.saveAll(conversationId, messages);
        }

        @Override
        public void deleteByConversationId(String conversationId) {
            delegate.deleteByConversationId(conversationId);
        }
    }
}
