package io.ddd4j.ai.extension.agent.autoconfigure;

import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.harness.agent.HarnessAgent;
import io.ddd4j.ai.extension.agent.service.AgentService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link AgentAutoConfiguration} 装配测试：缺凭证回退 / 配凭证装配 / 业务覆盖。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class AgentAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AgentAutoConfiguration.class));

    @Test
    void defaultContext_withoutApiKey_backsOff() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(HarnessAgent.class);
            assertThat(context).doesNotHaveBean(AgentService.class);
        });
    }

    @Test
    void withApiKey_exposesHarnessAgentAndAgentService() {
        contextRunner
                .withPropertyValues("ddd4j.ai.agent.api-key=test-key")
                .run(context -> {
                    assertThat(context).hasSingleBean(HarnessAgent.class);
                    assertThat(context).hasSingleBean(Toolkit.class);
                    assertThat(context).hasSingleBean(Model.class);
                    assertThat(context).hasSingleBean(AgentService.class);
                });
    }

    @Test
    void userProvidedModel_usedInsteadOfOpenAI() {
        contextRunner
                .withBean(Model.class, () -> mock(Model.class))
                .withPropertyValues("ddd4j.ai.agent.api-key=test-key")
                .run(context -> assertThat(context.getBean(Model.class)).isNotNull());
    }

    @Test
    void userProvidedAgentService_backsOff() {
        contextRunner
                .withBean(AgentService.class, () -> mock(AgentService.class))
                .withPropertyValues("ddd4j.ai.agent.api-key=test-key")
                .run(context -> assertThat(context.getBean(AgentService.class)).isNotNull());
    }

    @Test
    void backsOffWhenDisabled() {
        contextRunner
                .withPropertyValues("ddd4j.ai.agent.enabled=false", "ddd4j.ai.agent.api-key=test-key")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(HarnessAgent.class);
                    assertThat(context).doesNotHaveBean(AgentService.class);
                });
    }

    @Test
    void toolCallbacksInjected_registeredAsAgentscopeTools() {
        contextRunner
                .withBean(ToolCallback.class, () -> stubCallback())
                .withPropertyValues("ddd4j.ai.agent.api-key=test-key")
                .run(context -> assertThat(context).hasSingleBean(Toolkit.class));
    }

    private static ToolCallback stubCallback() {
        ToolCallback callback = mock(ToolCallback.class);
        org.springframework.ai.tool.definition.ToolDefinition def =
                mock(org.springframework.ai.tool.definition.ToolDefinition.class);
        when(callback.getToolDefinition()).thenReturn(def);
        when(def.name()).thenReturn("stub");
        when(def.description()).thenReturn("stub");
        when(def.inputSchema()).thenReturn("{}");
        return callback;
    }
}
