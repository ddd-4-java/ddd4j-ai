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
import static org.mockito.Mockito.verify;
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

    // ---- streamEvents：细粒度事件流（io.agentscope.core.event.AgentEvent）----
    // 注意：本文件已 import 粗粒度的 io.agentscope.core.agent.Event（简单名 Event），
    // 与细粒度的 io.agentscope.core.event.AgentEvent 是 Agentscope 的两套并行抽象，
    // 简单名不同故不冲突，此处用全限定名以免读者混淆。

    @Test
    void streamEvents_delegatesToHarnessAgent() {
        HarnessAgent harness = mock(HarnessAgent.class);
        var delta = new io.agentscope.core.event.TextBlockDeltaEvent("r1", "b1", "你好");
        when(harness.streamEvents(any(Msg.class)))
                .thenReturn(Flux.<io.agentscope.core.event.AgentEvent>just(delta));

        AgentScopeAgentAdapter adapter = new AgentScopeAgentAdapter(harness);
        List<io.agentscope.core.event.AgentEvent> events =
                adapter.streamEvents(io.ddd4j.ai.extension.agent.service.AgentTask.of("hi"))
                        .collectList().block();

        assertThat(events).hasSize(1);
        assertThat(((io.agentscope.core.event.TextBlockDeltaEvent) events.get(0)).getDelta())
                .isEqualTo("你好");
        verify(harness).streamEvents(any(Msg.class));
    }

    @Test
    void streamEvents_preservesOrderAndFidelity() {
        HarnessAgent harness = mock(HarnessAgent.class);
        var e1 = new io.agentscope.core.event.TextBlockDeltaEvent("r1", "b1", "你");
        var e2 = new io.agentscope.core.event.TextBlockDeltaEvent("r1", "b1", "好");
        var e3 = new io.agentscope.core.event.TextBlockDeltaEvent("r1", "b1", "！");
        when(harness.streamEvents(any(Msg.class)))
                .thenReturn(Flux.<io.agentscope.core.event.AgentEvent>just(e1, e2, e3));

        AgentScopeAgentAdapter adapter = new AgentScopeAgentAdapter(harness);
        List<io.agentscope.core.event.AgentEvent> events =
                adapter.streamEvents(io.ddd4j.ai.extension.agent.service.AgentTask.of("hi"))
                        .collectList().block();

        // 方案 B 的核心断言：零映射、顺序与实例完全保真
        assertThat(events).containsExactly(e1, e2, e3);
    }

    @Test
    void streamEvents_mapsErrorToAgentExecutionException() {
        HarnessAgent harness = mock(HarnessAgent.class);
        when(harness.streamEvents(any(Msg.class)))
                .thenReturn(Flux.<io.agentscope.core.event.AgentEvent>error(
                        new IllegalStateException("streamEvents down")));

        AgentScopeAgentAdapter adapter = new AgentScopeAgentAdapter(harness);

        assertThatThrownBy(() -> adapter.streamEvents(
                        io.ddd4j.ai.extension.agent.service.AgentTask.of("hi"))
                .collectList().block())
                .isInstanceOf(AgentExecutionException.class)
                .hasMessageContaining("streamEvents down");
    }

    @Test
    void streamEvents_nullTask_throwsNpe() {
        AgentScopeAgentAdapter adapter =
                new AgentScopeAgentAdapter(mock(HarnessAgent.class));

        assertThatThrownBy(() -> adapter.streamEvents(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void executeAsync_nonBlockingThread_succeeds() {
        HarnessAgent harness = mock(HarnessAgent.class);
        // 加延迟：瞬时完成会掩盖 NonBlocking 检查，必须让调用真的耗时
        when(harness.call(any(Msg.class)))
                .thenReturn(Mono.<Msg>just(new AssistantMessage("async answer"))
                        .delayElement(java.time.Duration.ofMillis(200)));

        AgentScopeAgentAdapter adapter = new AgentScopeAgentAdapter(harness);

        String output = adapter
                .executeAsync(io.ddd4j.ai.extension.agent.service.AgentTask.of("hi"))
                .subscribeOn(reactor.core.scheduler.Schedulers.parallel())
                .map(r -> r.output())
                .block();

        assertThat(output).isEqualTo("async answer");
    }

    @Test
    void execute_onNonBlockingThread_throwsGuidedError() {
        HarnessAgent harness = mock(HarnessAgent.class);
        when(harness.call(any(Msg.class)))
                .thenReturn(Mono.<Msg>just(new AssistantMessage("x"))
                        .delayElement(java.time.Duration.ofMillis(200)));

        AgentScopeAgentAdapter adapter = new AgentScopeAgentAdapter(harness);

        assertThatThrownBy(() -> Mono.fromCallable(() -> adapter.execute(
                        io.ddd4j.ai.extension.agent.service.AgentTask.of("hi")))
                .subscribeOn(reactor.core.scheduler.Schedulers.parallel())
                .block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("executeAsync");
    }
}
