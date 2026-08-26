package io.ddd4j.ai.extension.chat.autoconfigure;

import io.ddd4j.ai.extension.chat.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * {@link ChatAutoConfiguration} 装配测试。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class ChatAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ChatAutoConfiguration.class))
            .withBean("chatClientBuilder", ChatClient.Builder.class,
                    () -> ChatClient.builder(mock(ChatModel.class)));

    @Test
    void registersChatServiceWhenBuilderPresent() {
        runner.run(context -> assertThat(context).hasSingleBean(ChatService.class));
    }

    @Test
    void backsOffWhenDisabled() {
        runner.withPropertyValues("ddd4j.ai.chat.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ChatService.class));
    }

    @Test
    void backsOffWhenUserDefinesOwnChatService() {
        runner.withBean("customChatService", ChatService.class, CustomChatService::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatService.class);
                    assertThat(context.getBean(ChatService.class).chat("x")).isEqualTo("custom");
                });
    }

    static class CustomChatService implements ChatService {

        @Override
        public String chat(String message) {
            return "custom";
        }

        @Override
        public String chat(String message, String conversationId) {
            return "custom";
        }

        @Override
        public reactor.core.publisher.Flux<String> streamChat(String message) {
            return reactor.core.publisher.Flux.just("custom");
        }
    }
}
