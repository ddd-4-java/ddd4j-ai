package io.ddd4j.ai.extension.router.service.impl;

import io.ddd4j.ai.extension.router.service.RoutingStrategy;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询策略：候选模型依次循环，保证负载均匀分布。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class RoundRobinStrategy implements RoutingStrategy {

    /**
     * 构造 RoundRobinStrategy 对象。
     */
    public RoundRobinStrategy() {
    }

    private final AtomicInteger counter = new AtomicInteger();

    /**
     * 按内部计数器取模轮询选取候选模型。
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
        int index = Math.floorMod(counter.getAndIncrement(), candidates.size());
        return candidates.get(index);
    }
}
