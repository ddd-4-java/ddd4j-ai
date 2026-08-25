package io.ddd4j.ai.cmpt.router.service.impl;

import io.ddd4j.ai.cmpt.router.service.RoutingStrategy;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 权重策略：按模型权重比例随机分发（无权重候选退化为轮询兜底）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class WeightedStrategy implements RoutingStrategy {

    private final Map<String, Integer> weights;
    private final AtomicInteger counter = new AtomicInteger();
    private final Random random;

    public WeightedStrategy(Map<String, Integer> weights) {
        this(weights, new Random());
    }

    WeightedStrategy(Map<String, Integer> weights, Random random) {
        this.weights = weights == null ? Map.of() : Map.copyOf(weights);
        this.random = random;
    }

    @Override
    public String select(List<String> candidates) {
        Objects.requireNonNull(candidates, "candidates");
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("candidates must not be empty");
        }
        List<String> weighted = candidates.stream().filter(weights::containsKey).toList();
        if (weighted.isEmpty()) {
            return roundRobin(candidates);
        }
        int total = weighted.stream().mapToInt(weights::get).sum();
        if (total <= 0) {
            return roundRobin(candidates);
        }
        int roll = random.nextInt(total);
        int accumulated = 0;
        for (String modelId : weighted) {
            accumulated += weights.get(modelId);
            if (roll < accumulated) {
                return modelId;
            }
        }
        return weighted.get(weighted.size() - 1);
    }

    private String roundRobin(List<String> candidates) {
        int index = Math.floorMod(counter.getAndIncrement(), candidates.size());
        return candidates.get(index);
    }
}
