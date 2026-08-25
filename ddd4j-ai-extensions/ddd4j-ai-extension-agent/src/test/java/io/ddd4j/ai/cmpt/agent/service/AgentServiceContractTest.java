package io.ddd4j.ai.cmpt.agent.service;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link AgentService} 端口契约测试：任务校验 / 结果结构 / 流式步骤 / 异常传播。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class AgentServiceContractTest {

    static class FakeAgentService implements AgentService {

        private final RuntimeException failure;

        FakeAgentService() {
            this(null);
        }

        FakeAgentService(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public AgentResult execute(AgentTask task) {
            if (failure != null) {
                throw failure;
            }
            return new AgentResult("done", List.of(new AgentStep("result", "done", 0)), task.conversationId());
        }

        @Override
        public Flux<AgentStep> stream(AgentTask task) {
            return Flux.fromIterable(execute(task).steps());
        }
    }

    @Test
    void execute_returnsResultWithTrajectory() throws Exception {
        AgentResult result = new FakeAgentService().execute(AgentTask.of("task"));
        assertThat(result.output()).isEqualTo("done");
        assertThat(result.steps()).extracting(AgentStep::type).containsExactly("result");
    }

    @Test
    void stream_emitsSteps() {
        List<AgentStep> steps = new FakeAgentService().stream(AgentTask.of("task")).collectList().block();
        assertThat(steps).hasSize(1);
    }

    @Test
    void execute_propagatesEngineFailure() {
        RuntimeException cause = new IllegalStateException("agent down");
        assertThatThrownBy(() -> new FakeAgentService(cause).execute(AgentTask.of("task")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("agent down");
    }

    @Test
    void agentTask_rejectsBlankInstruction() {
        assertThatThrownBy(() -> new AgentTask("  ", Map.of(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void agentResult_defensivelyCopiesSteps() {
        List<AgentStep> mutable = new java.util.ArrayList<>();
        mutable.add(new AgentStep("result", "x", 0));
        AgentResult result = new AgentResult("x", mutable, null);
        mutable.clear();
        assertThat(result.steps()).hasSize(1);
    }
}
