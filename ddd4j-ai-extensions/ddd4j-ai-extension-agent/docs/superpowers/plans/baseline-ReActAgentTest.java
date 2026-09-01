package io.ddd4j.ai.extension.agent.service.impl;

import io.ddd4j.ai.extension.agent.service.AgentResult;
import io.ddd4j.ai.extension.agent.service.AgentStep;
import io.ddd4j.ai.extension.agent.service.AgentTask;
import io.ddd4j.ai.extension.chat.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ReActAgent} 单元测试：工具调用循环 / 直达答案 / 超限兜底 / 未知工具。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class ReActAgentTest {

    private ChatService chatService;
    private ToolCallback echoTool;

    @BeforeEach
    void setUp() {
        chatService = mock(ChatService.class);
        echoTool = mock(ToolCallback.class);
        when(echoTool.getToolDefinition()).thenReturn(ToolDefinition.builder()
                .name("echo")
                .description("echo")
                .inputSchema("{}")
                .build());
        when(echoTool.call(anyString())).thenAnswer(inv -> "observed:" + inv.getArgument(0));
    }

    @Test
    void execute_withToolCall_loopsThenReturnsResult() throws Exception {
        when(chatService.chat(anyString(), any())).thenReturn(
                "Thought: need tool\nAction: echo\nAction Input: {\"msg\":\"hi\"}",
                "Final answer: done");
        ReActAgent agent = new ReActAgent(chatService, List.of(echoTool), 5);

        AgentResult result = agent.execute(AgentTask.of("task"));

        assertThat(result.output()).isEqualTo("Final answer: done");
        assertThat(result.steps()).extracting(AgentStep::type)
                .containsExactly("thought", "observation", "thought", "result");
        verify(echoTool).call("{\"msg\":\"hi\"}");
    }

    @Test
    void execute_withoutToolCall_returnsImmediately() throws Exception {
        when(chatService.chat(anyString(), any())).thenReturn("plain answer");
        ReActAgent agent = new ReActAgent(chatService, List.of(), 5);

        AgentResult result = agent.execute(AgentTask.of("task"));

        assertThat(result.output()).isEqualTo("plain answer");
        assertThat(result.steps()).extracting(AgentStep::type).containsExactly("thought", "result");
    }

    @Test
    void execute_exceedsMaxIterations_throws() {
        when(chatService.chat(anyString(), any())).thenReturn("Action: echo\nAction Input: {}");
        ReActAgent agent = new ReActAgent(chatService, List.of(echoTool), 2);

        assertThatThrownBy(() -> agent.execute(AgentTask.of("task")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("最大迭代次数");
    }

    @Test
    void execute_unknownTool_becomesObservation() throws Exception {
        when(chatService.chat(anyString(), any())).thenReturn(
                "Action: nope\nAction Input: {}",
                "recovered");
        ReActAgent agent = new ReActAgent(chatService, List.of(echoTool), 5);

        AgentResult result = agent.execute(AgentTask.of("task"));

        assertThat(result.steps()).extracting(AgentStep::type)
                .containsExactly("thought", "observation", "thought", "result");
        assertThat(result.steps().get(1).content()).contains("未知工具").contains("nope");
    }

    @Test
    void execute_toolFailure_becomesObservation() throws Exception {
        when(chatService.chat(anyString(), any())).thenReturn(
                "Action: echo\nAction Input: {}",
                "recovered");
        when(echoTool.call(anyString())).thenThrow(new IllegalStateException("boom"));
        ReActAgent agent = new ReActAgent(chatService, List.of(echoTool), 5);

        AgentResult result = agent.execute(AgentTask.of("task"));

        assertThat(result.steps()).extracting(AgentStep::type)
                .containsExactly("thought", "observation", "thought", "result");
        assertThat(result.steps().get(1).content()).contains("工具执行失败").contains("boom");
    }

    @Test
    void stream_emitsSteps() {
        when(chatService.chat(anyString(), any())).thenReturn("answer");
        ReActAgent agent = new ReActAgent(chatService, List.of(), 5);

        List<AgentStep> steps = agent.stream(AgentTask.of("task")).collectList().block();

        assertThat(steps).hasSize(2);
    }
}
