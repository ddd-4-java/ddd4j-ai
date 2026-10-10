package io.ddd4j.ai.extension.router.service.impl;

import io.ddd4j.ai.extension.router.service.ChatRouter;
import io.ddd4j.ai.extension.router.service.RoutingStrategy;
import io.ddd4j.ai.core.AiRequest;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 多模型路由实现：持有 ChatClient 池（bean 名 → 客户端），按策略选中模型后转发请求，
 * 并把单轮响应耗时回写策略（供延迟策略学习）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class MultiModelChatRouter implements ChatRouter {

    private final Map<String, ChatClient> clients;
    private final RoutingStrategy strategy;

    /**
     * 构造多模型路由器：ChatClient 池非空、策略非 null。
     *
     * @param clients  模型 id → ChatClient 映射（复制为不可变视图）
     * @param strategy 分发策略
     * @throws IllegalArgumentException clients 为 null 或空时
     * @throws NullPointerException     strategy 为 null 时
     */
    public MultiModelChatRouter(Map<String, ChatClient> clients, RoutingStrategy strategy) {
        if (clients == null || clients.isEmpty()) {
            throw new IllegalArgumentException("clients must not be empty");
        }
        this.clients = Collections.unmodifiableMap(new java.util.LinkedHashMap<>(clients));
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    /**
     * 同步路由：策略选中模型后转发，并在 finally 中回写耗时供延迟策略学习。
     *
     * @param request 对话请求
     * @return 模型生成的完整回复
     * @throws IllegalArgumentException 无可用客户端时
     */
    @Override
    public String route(AiRequest request) {
        String modelId = strategy.select(List.copyOf(clients.keySet()));
        long start = System.nanoTime();
        try {
            return clients.get(modelId).prompt().user(request.input()).call().content();
        } finally {
            strategy.recordLatency(modelId, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
        }
    }

    /**
     * 流式路由：策略选中模型后转发为增量流（流式路径不回写耗时）。
     *
     * @param request 对话请求
     * @return 回答增量流
     * @throws IllegalArgumentException 无可用客户端时
     */
    @Override
    public Flux<String> streamRoute(AiRequest request) {
        String modelId = strategy.select(List.copyOf(clients.keySet()));
        return clients.get(modelId).prompt().user(request.input()).stream().content();
    }
}
