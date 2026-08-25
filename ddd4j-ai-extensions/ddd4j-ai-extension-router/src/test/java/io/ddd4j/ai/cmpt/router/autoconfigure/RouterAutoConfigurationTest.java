package io.ddd4j.ai.cmpt.router.autoconfigure;

import io.ddd4j.ai.cmpt.router.service.ChatRouter;
import io.ddd4j.ai.cmpt.router.service.RoutingStrategy;
import io.ddd4j.ai.cmpt.router.service.impl.LatencyStrategy;
import io.ddd4j.ai.cmpt.router.service.impl.RoundRobinStrategy;
import io.ddd4j.ai.cmpt.router.service.impl.WeightedStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * {@link RouterAutoConfiguration} 装配测试：缺客户端回退 / 默认轮询 / 策略属性切换 / 关闭回退。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class RouterAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RouterAutoConfiguration.class));

    @Test
    void backsOffWithoutChatClient() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(ChatRouter.class));
    }

    @Test
    void registersRouterWithChatClients_defaultRoundRobin() {
        contextRunner
                .withBean("modelA", ChatClient.class, () -> mock(ChatClient.class))
                .withBean("modelB", ChatClient.class, () -> mock(ChatClient.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatRouter.class);
                    assertThat(context).hasSingleBean(RoutingStrategy.class);
                    assertThat(context.getBean(RoutingStrategy.class)).isInstanceOf(RoundRobinStrategy.class);
                });
    }

    @Test
    void weightedStrategySelectedViaProperty() {
        contextRunner
                .withBean("modelA", ChatClient.class, () -> mock(ChatClient.class))
                .withPropertyValues("ddd4j.ai.router.strategy=WEIGHTED", "ddd4j.ai.router.weights.modelA=3")
                .run(context -> assertThat(context.getBean(RoutingStrategy.class)).isInstanceOf(WeightedStrategy.class));
    }

    @Test
    void latencyStrategySelectedViaProperty() {
        contextRunner
                .withBean("modelA", ChatClient.class, () -> mock(ChatClient.class))
                .withPropertyValues("ddd4j.ai.router.strategy=LATENCY")
                .run(context -> assertThat(context.getBean(RoutingStrategy.class)).isInstanceOf(LatencyStrategy.class));
    }

    @Test
    void backsOffWhenDisabled() {
        contextRunner
                .withBean("modelA", ChatClient.class, () -> mock(ChatClient.class))
                .withPropertyValues("ddd4j.ai.router.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ChatRouter.class));
    }
}
