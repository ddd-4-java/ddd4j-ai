package io.ddd4j.ai.extension.router.service.impl;

import io.ddd4j.ai.extension.router.service.RoutingStrategy;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 延迟策略：优先选择最近响应最快（记录耗时最小）的模型；尚无记录时轮询兜底。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class LatencyStrategy implements RoutingStrategy {

    private final ConcurrentHashMap<String, Long> latencies = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public String select(List<String> candidates) {
        Objects.requireNonNull(candidates, "candidates");
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("candidates must not be empty");
        }
        List<String> measured = candidates.stream().filter(latencies::containsKey).toList();
        if (measured.isEmpty()) {
            int index = Math.floorMod(counter.getAndIncrement(), candidates.size());
            return candidates.get(index);
        }
        return measured.stream().min(Comparator.comparingLong(latencies::get)).orElseThrow();
    }

    @Override
    public void recordLatency(String modelId, long millis) {
        latencies.put(modelId, millis);
    }
}
