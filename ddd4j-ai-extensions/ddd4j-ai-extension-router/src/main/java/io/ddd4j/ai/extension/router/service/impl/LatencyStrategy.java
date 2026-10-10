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

    /**
     * 构造 LatencyStrategy 对象。
     */
    public LatencyStrategy() {
    }

    private final ConcurrentHashMap<String, Long> latencies = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger();

    /**
     * 选取记录耗时最小的模型；全部候选尚无耗时记录时按轮询兜底。
     *
     * @param candidates 候选模型 id 列表（非空）
     * @return 选中的模型 id
     * @throws NullPointerException  candidates 为 null 时
     * @throws IllegalArgumentException candidates 为空时
     */
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

    /**
     * 回写某模型最近一次响应耗时（覆盖旧值）。
     *
     * @param modelId 模型 id
     * @param millis  响应耗时（毫秒）
     */
    @Override
    public void recordLatency(String modelId, long millis) {
        latencies.put(modelId, millis);
    }
}
