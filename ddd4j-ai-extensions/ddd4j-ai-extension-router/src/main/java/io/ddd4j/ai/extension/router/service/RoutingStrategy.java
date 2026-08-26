package io.ddd4j.ai.extension.router.service;

import java.util.List;

/**
 * 模型选择策略：从候选模型 id 列表中选一个。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public interface RoutingStrategy {

    /**
     * 从候选列表中选中一个模型 id。
     *
     * @param candidates 候选模型 id（非空）
     * @return 选中模型 id
     */
    String select(List<String> candidates);

    /**
     * 回写一次请求的响应耗时（延迟策略使用；其余策略可忽略）。
     *
     * @param modelId 模型 id
     * @param millis  响应毫秒数
     */
    default void recordLatency(String modelId, long millis) {
        // no-op
    }
}
