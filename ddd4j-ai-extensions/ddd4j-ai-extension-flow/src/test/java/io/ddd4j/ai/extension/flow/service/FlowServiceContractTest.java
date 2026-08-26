package io.ddd4j.ai.extension.flow.service;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link FlowService} 端口契约测试：运行语义 / 流式 / 异常传播 / DSL 校验。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class FlowServiceContractTest {

    static class FakeFlowService implements FlowService {

        private final RuntimeException failure;

        FakeFlowService() {
            this(null);
        }

        FakeFlowService(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public CompiledGraph compile(FlowDefinition definition) {
            return null;
        }

        @Override
        public Map<String, Object> run(CompiledGraph graph, Map<String, Object> input) {
            if (failure != null) {
                throw failure;
            }
            return Map.of("out", "done");
        }

        @Override
        public Flux<NodeOutput> stream(CompiledGraph graph, Map<String, Object> input) {
            return Flux.fromIterable(run(graph, input).entrySet())
                    .map(entry -> NodeOutput.of("node", null, null, null));
        }
    }

    @Test
    void run_returnsFinalState() {
        Map<String, Object> state = new FakeFlowService().run(null, Map.of("in", "x"));
        assertThat(state).containsEntry("out", "done");
    }

    @Test
    void run_propagatesEngineFailure() {
        RuntimeException cause = new IllegalStateException("graph down");
        assertThatThrownBy(() -> new FakeFlowService(cause).run(null, Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("graph down");
    }

    @Test
    void flowDefinition_rejectsNullName() {
        assertThatThrownBy(() -> new FlowDefinition(null, java.util.List.of(), java.util.List.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void flowNodeSpec_rejectsNullId() {
        assertThatThrownBy(() -> new FlowNodeSpec(null, FlowNodeType.LLM, "p", null, null, "out", Map.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void flowNodeSpec_defensivelyCopiesBranches() {
        Map<String, String> branches = new java.util.HashMap<>();
        branches.put("a", "next");
        FlowNodeSpec spec = new FlowNodeSpec("n", FlowNodeType.BRANCH, null, null, "k", null, branches);
        branches.clear();
        assertThat(spec.branches()).containsEntry("a", "next");
    }
}
