package io.ddd4j.ai.extension.agent.autoconfigure;

import io.ddd4j.ai.extension.agent.service.AgentService;
import io.ddd4j.ai.extension.agent.service.impl.PlanExecuteAgent;
import io.ddd4j.ai.extension.agent.service.impl.ReActAgent;
import io.ddd4j.ai.extension.chat.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AgentAutoConfiguration} 装配测试：缺依赖回退 / 默认装配 / 关闭回退。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class AgentAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AgentAutoConfiguration.class));

    @Test
    void backsOffWithoutChatService() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(AgentService.class));
    }

    @Test
    void registersAgentsWithChatService() {
        contextRunner.withBean(ChatService.class, AgentAutoConfigurationTest::stubChatService)
                .run(context -> {
                    assertThat(context).hasSingleBean(ReActAgent.class);
                    assertThat(context).hasSingleBean(PlanExecuteAgent.class);
                    assertThat(context.getBean(AgentService.class)).isInstanceOf(ReActAgent.class);
                });
    }

    @Test
    void backsOffWhenDisabled() {
        contextRunner.withBean(ChatService.class, AgentAutoConfigurationTest::stubChatService)
                .withPropertyValues("ddd4j.ai.agent.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(AgentService.class));
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
