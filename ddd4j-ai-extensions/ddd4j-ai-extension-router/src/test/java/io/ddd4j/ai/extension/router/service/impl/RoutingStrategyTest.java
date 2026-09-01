package io.ddd4j.ai.extension.router.service.impl;

import io.ddd4j.ai.extension.router.service.RoutingStrategy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 路由策略单元测试：轮询均匀 / 权重倾斜 / 延迟优先 / 空候选兜底。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class RoutingStrategyTest {

    private final List<String> candidates = List.of("a", "b", "c");

    @Test
    void roundRobin_distributesEvenly() {
        RoundRobinStrategy strategy = new RoundRobinStrategy();
        assertThat(strategy.select(candidates)).isEqualTo("a");
        assertThat(strategy.select(candidates)).isEqualTo("b");
        assertThat(strategy.select(candidates)).isEqualTo("c");
        assertThat(strategy.select(candidates)).isEqualTo("a");
    }

    @Test
    void roundRobin_rejectsEmptyCandidates() {
        RoundRobinStrategy strategy = new RoundRobinStrategy();
        assertThatThrownBy(() -> strategy.select(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void weighted_prefersHigherWeight() {
        WeightedStrategy strategy = new WeightedStrategy(Map.of("a", 4, "b", 1), new Random(42));
        int a = 0;
        int b = 0;
        for (int i = 0; i < 200; i++) {
            String selected = strategy.select(candidates);
            if ("a".equals(selected)) {
                a++;
            } else if ("b".equals(selected)) {
                b++;
            }
        }
        assertThat(a).isGreaterThan(b);
    }

    @Test
    void weighted_fallsBackToRoundRobinWithoutWeights() {
        WeightedStrategy strategy = new WeightedStrategy(Map.of());
        assertThat(strategy.select(candidates)).isEqualTo("a");
        assertThat(strategy.select(candidates)).isEqualTo("b");
    }

    @Test
    void latency_selectsFastestMeasured() {
        LatencyStrategy strategy = new LatencyStrategy();
        strategy.recordLatency("a", 100);
        strategy.recordLatency("b", 40);
        assertThat(strategy.select(candidates)).isEqualTo("b");
    }

    @Test
    void latency_fallsBackToRoundRobinBeforeMeasurements() {
        LatencyStrategy strategy = new LatencyStrategy();
        assertThat(strategy.select(candidates)).isEqualTo("a");
        assertThat(strategy.select(candidates)).isEqualTo("b");
    }

    @Test
    void strategies_rejectEmptyCandidates() {
        List<RoutingStrategy> strategies = List.of(
                new RoundRobinStrategy(),
                new WeightedStrategy(Map.of()),
                new LatencyStrategy());
        for (RoutingStrategy strategy : strategies) {
            assertThatThrownBy(() -> strategy.select(List.of()))
                    .as(strategy.getClass().getSimpleName())
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
