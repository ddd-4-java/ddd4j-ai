package io.ddd4j.ai.samples.router;

import io.ddd4j.ai.extension.router.service.ChatRouter;
import io.ddd4j.ai.core.AiRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * router 多模型路由组件使用示例：按策略（轮询/权重/延迟）在模型池中分发。
 *
 * <p>前提：引入 {@code ddd4j-ai-extension-router}，业务侧以不同 bean 名注册多个
 * {@code ChatClient}（bean 名即模型 id），策略经 {@code ddd4j.ai.router.strategy} 配置。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Component
public class RouterSample {

    private final ChatRouter chatRouter;

    public RouterSample(ChatRouter chatRouter) {
        this.chatRouter = chatRouter;
    }

    /** 路由到策略选中的模型完成单轮问答。 */
    public String ask(String message) {
        return chatRouter.route(AiRequest.of(message));
    }

    /** 路由流式问答。 */
    public Flux<String> askStream(String message) {
        return chatRouter.streamRoute(AiRequest.of(message));
    }
}
