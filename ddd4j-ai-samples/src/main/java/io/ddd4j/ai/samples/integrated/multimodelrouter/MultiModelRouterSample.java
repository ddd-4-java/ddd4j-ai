package io.ddd4j.ai.samples.integrated.multimodelrouter;

import io.ddd4j.ai.extension.router.service.ChatRouter;
import io.ddd4j.ai.extension.chat.service.ChatService;
import io.ddd4j.ai.core.AiRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 场景 5：多模型路由（router + chat）。
 * <p>
 * 展示：按策略（轮询/权重/延迟）在多个 ChatClient 模型池中分发请求。
 */
@Component
public class MultiModelRouterSample {

    private final ChatRouter chatRouter;

    public MultiModelRouterSample(ChatRouter chatRouter) {
        this.chatRouter = chatRouter;
    }

    /**
     * 路由请求：按策略自动选择模型并返回结果。
     */
    public String route(String message) {
        return chatRouter.route(AiRequest.of(message));
    }

    /**
     * 路由流式请求：按策略选择模型并逐 token 返回。
     */
    public Flux<String> routeStream(String message) {
        return chatRouter.streamRoute(AiRequest.of(message));
    }
}
