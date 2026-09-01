package io.ddd4j.ai.extension.agent.agent;

import java.util.List;
import java.util.Map;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.ToolCallParam;
import io.agentscope.core.tool.Toolkit;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link SpringAiToolkitBuilder} 单元测试：Spring AI ToolCallback → Agentscope AgentTool。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class SpringAiToolkitBuilderTest {

    private static ToolCallback stubCallback(String name, String description, String result) {
        ToolCallback callback = mock(ToolCallback.class);
        ToolDefinition def = mock(ToolDefinition.class);
        when(callback.getToolDefinition()).thenReturn(def);
        when(def.name()).thenReturn(name);
        when(def.description()).thenReturn(description);
        when(def.inputSchema()).thenReturn("{}");
        when(callback.call(anyString())).thenReturn(result);
        return callback;
    }

    @Test
    void register_invokesCallback() throws Exception {
        ToolCallback echo = stubCallback("echo", "echo back", "hi");
        Toolkit toolkit = new Toolkit();
        SpringAiToolkitBuilder.registerSpringAiTools(List.of(echo), toolkit);

        assertThat(toolkit.getToolNames()).contains("echo");
        AgentTool registered = toolkit.getTool("echo");
        ToolResultBlock result = registered.callAsync(
                ToolCallParam.builder().input(Map.of("msg", "hi")).build()).block();

        assertThat(result).isNotNull();
        assertThat(result.getOutput()).isNotEmpty();
    }

    @Test
    void register_callbackError_returnsErrorBlock() throws Exception {
        ToolCallback broken = stubCallback("broken", "broken tool", null);
        when(broken.call(anyString())).thenThrow(new IllegalStateException("boom"));
        Toolkit toolkit = new Toolkit();
        SpringAiToolkitBuilder.registerSpringAiTools(List.of(broken), toolkit);

        AgentTool registered = toolkit.getTool("broken");
        ToolResultBlock result = registered.callAsync(
                ToolCallParam.builder().input(Map.of()).build()).block();

        assertThat(result.getOutput().get(0).toString()).contains("boom");
    }

    @Test
    void register_emptyList_keepsToolkitEmpty() {
        Toolkit toolkit = new Toolkit();
        SpringAiToolkitBuilder.registerSpringAiTools(List.of(), toolkit);
        assertThat(toolkit.getToolNames()).isEmpty();
    }

    @Test
    void register_nullList_keepsToolkitEmpty() {
        Toolkit toolkit = new Toolkit();
        SpringAiToolkitBuilder.registerSpringAiTools(null, toolkit);
        assertThat(toolkit.getToolNames()).isEmpty();
    }
}
