package io.ddd4j.ai.extension.flow.autoconfigure;

import io.ddd4j.ai.extension.chat.service.ChatService;
import io.ddd4j.ai.extension.flow.service.FlowService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link FlowAutoConfiguration} 装配测试：缺依赖回退 / 默认装配 / 关闭回退。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class FlowAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FlowAutoConfiguration.class));

    @Test
    void backsOffWithoutChatService() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(FlowService.class));
    }

    @Test
    void registersFlowServiceWithChatService() {
        contextRunner.withBean(ChatService.class, FlowAutoConfigurationTest::stubChatService)
                .run(context -> assertThat(context).hasSingleBean(FlowService.class));
    }

    @Test
    void backsOffWhenDisabled() {
        contextRunner.withBean(ChatService.class, FlowAutoConfigurationTest::stubChatService)
                .withPropertyValues("ddd4j.ai.flow.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(FlowService.class));
    }

    private static ChatService stubChatService() {
        return new ChatService() {
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
        };
    }
}
