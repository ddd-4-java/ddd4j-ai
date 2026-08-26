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

    private final AtomicInteger counter = new AtomicInteger();

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
