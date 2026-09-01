package io.ddd4j.ai.extension.agent.agent;

import java.util.List;

import io.agentscope.core.agent.Event;
import io.agentscope.core.message.AssistantMessage;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link AgentScopeAgentAdapter} 单元测试：把 Agentscope {@link HarnessAgent} 包装为
 * ddd4j-ai-extension-agent 的 {@link io.ddd4j.ai.extension.agent.service.AgentService} SPI。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class AgentScopeAgentAdapterTest {

    @Test
    void execute_returnsAgentResultFromAssistantMessage() {
        HarnessAgent harness = mock(HarnessAgent.class);
        AssistantMessage response = new AssistantMessage("hello back");
        when(harness.call(any(Msg.class))).thenReturn(Mono.just(response));

        AgentScopeAgentAdapter adapter = new AgentScopeAgentAdapter(harness);
        var result = adapter.execute(io.ddd4j.ai.extension.agent.service.AgentTask.of("hello"));

        assertThat(result.output()).isEqualTo("hello back");
        assertThat(result.conversationId()).isNull();
    }

    @Test
    void execute_harnessEmitsAssistantMessageAsResult() {
        HarnessAgent harness = mock(HarnessAgent.class);
        AssistantMessage response = new AssistantMessage("ok");
        when(harness.call(any(Msg.class))).thenReturn(Mono.just(response));

        AgentScopeAgentAdapter adapter = new AgentScopeAgentAdapter(harness);
        var result = adapter.execute(io.ddd4j.ai.extension.agent.service.AgentTask.of("test"));

        assertThat(result.steps()).extracting(s -> s.type())
                .contains("result");
    }

    @Test
    void execute_harnessCallFails_throwsAgentExecution() {
        HarnessAgent harness = mock(HarnessAgent.class);
        when(harness.call(any(Msg.class))).thenReturn(Mono.error(new IllegalStateException("down")));

        AgentScopeAgentAdapter adapter = new AgentScopeAgentAdapter(harness);

        assertThatThrownBy(() -> adapter.execute(io.ddd4j.ai.extension.agent.service.AgentTask.of("hi")))
                .isInstanceOf(AgentExecutionException.class)
                .hasMessageContaining("down");
    }

    @Test
    void stream_emitsEventsAsSteps() {
        HarnessAgent harness = mock(HarnessAgent.class);
        Event event = mock(Event.class);
        when(event.getType()).thenReturn(io.agentscope.core.agent.EventType.TOOL_RESULT);
        when(harness.stream(any(Msg.class), any(io.agentscope.core.agent.StreamOptions.class)))
                .thenReturn(Flux.just(event));

        AgentScopeAgentAdapter adapter = new AgentScopeAgentAdapter(harness);
        List<io.ddd4j.ai.extension.agent.service.AgentStep> steps =
                adapter.stream(io.ddd4j.ai.extension.agent.service.AgentTask.of("hi"))
                        .collectList().block();

        assertThat(steps).hasSize(1);
        assertThat(steps.get(0).type()).isEqualTo("observation");
    }

    @Test
    void stream_emptyEventsStream_completesEmpty() {
        HarnessAgent harness = mock(HarnessAgent.class);
        when(harness.stream(any(Msg.class), any(io.agentscope.core.agent.StreamOptions.class)))
                .thenReturn(Flux.<Event>empty());

        AgentScopeAgentAdapter adapter = new AgentScopeAgentAdapter(harness);
        List<io.ddd4j.ai.extension.agent.service.AgentStep> steps =
                adapter.stream(io.ddd4j.ai.extension.agent.service.AgentTask.of("hi"))
                        .collectList().block();
        assertThat(steps).isEmpty();
    }

    @Test
    void constructor_nullHarness_throws() {
        assertThatThrownBy(() -> new AgentScopeAgentAdapter(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void stream_streamError_propagatesAsAgentExecution() {
        HarnessAgent harness = mock(HarnessAgent.class);
        when(harness.stream(any(Msg.class), any(io.agentscope.core.agent.StreamOptions.class)))
                .thenReturn(Flux.<Event>error(new IllegalStateException("stream down")));

        AgentScopeAgentAdapter adapter = new AgentScopeAgentAdapter(harness);

        assertThatThrownBy(() -> adapter.stream(io.ddd4j.ai.extension.agent.service.AgentTask.of("hi"))
                .collectList().block())
                .isInstanceOf(AgentExecutionException.class);
    }
}
