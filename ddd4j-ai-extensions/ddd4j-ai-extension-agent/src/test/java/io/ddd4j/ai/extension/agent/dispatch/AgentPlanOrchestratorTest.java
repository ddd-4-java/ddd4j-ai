package io.ddd4j.ai.extension.agent.dispatch;

import java.util.List;
import java.util.Map;

import io.agentscope.core.message.AssistantMessage;
import io.agentscope.core.message.Msg;
import io.agentscope.harness.agent.HarnessAgent;
import io.ddd4j.ai.extension.agent.agent.AgentExecutionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link AgentPlanOrchestrator} 单元测试：提交/并行派发/合并/失败处理。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class AgentPlanOrchestratorTest {

    private static HarnessAgent stubHarness(java.util.Queue<String> replies) {
        HarnessAgent harness = mock(HarnessAgent.class);
        when(harness.call(any(Msg.class))).thenAnswer(inv -> {
            String next = replies.isEmpty() ? "fallback" : replies.poll();
            return reactor.core.publisher.Mono.just(new AssistantMessage(next));
        });
        return harness;
    }

    @Test
    void submitPlan_createsPendingTasks() {
        AgentPlanOrchestrator orchestrator = new AgentPlanOrchestrator(
                mock(HarnessAgent.class), new InMemoryAgentDispatchTaskRepository());

        String planId = orchestrator.submitPlan("make tea",
                List.of("boil water", "add leaves"));

        var tasks = orchestrator.tasksOf(planId);
        assertThat(tasks).hasSize(2);
        assertThat(tasks).allSatisfy(t -> assertThat(t.status()).isEqualTo(AgentDispatchTask.PENDING));
    }

    @Test
    void submitPlan_emptyInstructions_throws() {
        AgentPlanOrchestrator orchestrator = new AgentPlanOrchestrator(
                mock(HarnessAgent.class), new InMemoryAgentDispatchTaskRepository());
        assertThatThrownBy(() -> orchestrator.submitPlan("g", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dispatchAll_executesAllTasksAndWritesBack() {
        HarnessAgent harness = stubHarness(new java.util.LinkedList<>(List.of("water boiled", "tea ready")));
        InMemoryAgentDispatchTaskRepository repository = new InMemoryAgentDispatchTaskRepository();
        AgentPlanOrchestrator orchestrator = new AgentPlanOrchestrator(harness, repository);

        String planId = orchestrator.submitPlan("make tea", List.of("boil water", "add leaves"));
        Map<String, String> results = orchestrator.dispatchAll(planId);

        assertThat(results).hasSize(2).containsValue("water boiled").containsValue("tea ready");
        assertThat(orchestrator.tasksOf(planId)).allSatisfy(
                t -> assertThat(t.status()).isEqualTo(AgentDispatchTask.DONE));
    }

    @Test
    void dispatchAll_taskFailure_markedFailedNotThrown() {
        HarnessAgent harness = mock(HarnessAgent.class);
        when(harness.call(any(Msg.class)))
                .thenReturn(reactor.core.publisher.Mono.just(new AssistantMessage("ok")))
                .thenReturn(reactor.core.publisher.Mono.error(new IllegalStateException("tool down")));
        InMemoryAgentDispatchTaskRepository repository = new InMemoryAgentDispatchTaskRepository();
        AgentPlanOrchestrator orchestrator = new AgentPlanOrchestrator(harness, repository);

        String planId = orchestrator.submitPlan("goal", List.of("t1", "t2"));
        Map<String, String> results = orchestrator.dispatchAll(planId);

        assertThat(results).hasSize(2);
        var statuses = orchestrator.tasksOf(planId).stream().map(AgentDispatchTask::status).toList();
        assertThat(statuses).contains(AgentDispatchTask.FAILED);
    }

    @Test
    void mergeResults_allDone_synthesizes() {
        HarnessAgent harness = stubHarness(new java.util.LinkedList<>(
                List.of("r1", "r2", "merged final answer")));
        InMemoryAgentDispatchTaskRepository repository = new InMemoryAgentDispatchTaskRepository();
        AgentPlanOrchestrator orchestrator = new AgentPlanOrchestrator(harness, repository);

        String planId = orchestrator.submitPlan("goal", List.of("t1", "t2"));
        orchestrator.dispatchAll(planId);
        String merged = orchestrator.mergeResults(planId);

        assertThat(merged).isEqualTo("merged final answer");
    }

    @Test
    void mergeResults_withFailedTask_throws() {
        HarnessAgent harness = mock(HarnessAgent.class);
        when(harness.call(any(Msg.class)))
                .thenReturn(reactor.core.publisher.Mono.error(new IllegalStateException("down")));
        InMemoryAgentDispatchTaskRepository repository = new InMemoryAgentDispatchTaskRepository();
        AgentPlanOrchestrator orchestrator = new AgentPlanOrchestrator(harness, repository);

        String planId = orchestrator.submitPlan("goal", List.of("t1"));
        orchestrator.dispatchAll(planId);

        assertThatThrownBy(() -> orchestrator.mergeResults(planId))
                .isInstanceOf(AgentExecutionException.class)
                .hasMessageContaining("not fully done");
    }

    @Test
    void dispatchAll_unknownPlanId_throws() {
        AgentPlanOrchestrator orchestrator = new AgentPlanOrchestrator(
                mock(HarnessAgent.class), new InMemoryAgentDispatchTaskRepository());
        assertThatThrownBy(() -> orchestrator.dispatchAll("nope"))
                .isInstanceOf(AgentExecutionException.class)
                .hasMessageContaining("no pending tasks");
    }

    @Test
    void dispatchAllAsync_nonBlockingThread_succeeds() {
        HarnessAgent harness = mock(HarnessAgent.class);
        // 加延迟：瞬时完成会掩盖 NonBlocking 检查，必须让调用真的耗时
        when(harness.call(any(Msg.class)))
                .thenReturn(reactor.core.publisher.Mono.<Msg>just(new AssistantMessage("done"))
                        .delayElement(java.time.Duration.ofMillis(150)));
        AgentPlanOrchestrator orchestrator = new AgentPlanOrchestrator(
                harness, new InMemoryAgentDispatchTaskRepository());

        String planId = orchestrator.submitPlan("goal", List.of("t1", "t2"));

        Map<String, String> results = orchestrator.dispatchAllAsync(planId)
                .subscribeOn(reactor.core.scheduler.Schedulers.parallel())
                .block();

        assertThat(results).hasSize(2);
        assertThat(results.values()).allMatch("done"::equals);
    }

    @Test
    void dispatchAll_onNonBlockingThread_throwsGuidedError() {
        HarnessAgent harness = mock(HarnessAgent.class);
        when(harness.call(any(Msg.class)))
                .thenReturn(reactor.core.publisher.Mono.<Msg>just(new AssistantMessage("x"))
                        .delayElement(java.time.Duration.ofMillis(150)));
        AgentPlanOrchestrator orchestrator = new AgentPlanOrchestrator(
                harness, new InMemoryAgentDispatchTaskRepository());
        String planId = orchestrator.submitPlan("goal", List.of("t1"));

        assertThatThrownBy(() -> reactor.core.publisher.Mono
                .fromCallable(() -> orchestrator.dispatchAll(planId))
                .subscribeOn(reactor.core.scheduler.Schedulers.parallel())
                .block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dispatchAllAsync");
    }
}
