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
 * @author ddd4j
 */
public class MultiModelChatRouter implements ChatRouter {

    private final Map<String, ChatClient> clients;
    private final RoutingStrategy strategy;

    /**
     * 创建多模型路由器。
     *
     * @param clients 模型 id → ChatClient 映射，不允许为空
     * @param strategy 路由策略，不允许 {@code null}
     * @throws IllegalArgumentException {@code clients} 为 {@code null} 或空
     * @throws NullPointerException {@code strategy} 为 {@code null}
     */
    public MultiModelChatRouter(Map<String, ChatClient> clients, RoutingStrategy strategy) {
        if (clients == null || clients.isEmpty()) {
            throw new IllegalArgumentException("clients must not be empty");
        }
        this.clients = Collections.unmodifiableMap(new java.util.LinkedHashMap<>(clients));
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

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

    @Override
    public Flux<String> streamRoute(AiRequest request) {
        String modelId = strategy.select(List.copyOf(clients.keySet()));
        return clients.get(modelId).prompt().user(request.input()).stream().content();
    }
}
