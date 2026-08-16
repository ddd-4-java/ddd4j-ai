package io.ddd4j.ai.cmpt.rag.autoconfigure;

import io.ddd4j.ai.cmpt.chat.service.ChatService;
import io.ddd4j.ai.cmpt.rag.service.RagService;
import io.ddd4j.ai.cmpt.rag.service.Reranker;
import io.ddd4j.ai.cmpt.vectordb.service.VectorDbService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * {@link RagAutoConfiguration} 装配测试。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class RagAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RagAutoConfiguration.class))
            .withBean("chatService", ChatService.class, StubChatService::new)
            .withBean("vectorDbService", VectorDbService.class, () -> mock(VectorDbService.class));

    @Test
    void registersRagServiceWhenPortsPresent() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(RagService.class);
            assertThat(context).hasSingleBean(Reranker.class);   // 默认直通
        });
    }

    @Test
    void backsOffWhenDisabled() {
        runner.withPropertyValues("ddd4j.ai.rag.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(RagService.class));
    }

    @Test
    void backsOffWithoutChatService() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RagAutoConfiguration.class))
                .withBean("vectorDbService", VectorDbService.class, () -> mock(VectorDbService.class))
                .run(context -> assertThat(context).doesNotHaveBean(RagService.class));
    }

    static class StubChatService implements ChatService {

        @Override
        public String chat(String message) {
            return "stub";
        }

        @Override
        public String chat(String message, String conversationId) {
            return "stub";
        }

        @Override
        public Flux<String> streamChat(String message) {
            return Flux.just("stub");
        }
    }
}
