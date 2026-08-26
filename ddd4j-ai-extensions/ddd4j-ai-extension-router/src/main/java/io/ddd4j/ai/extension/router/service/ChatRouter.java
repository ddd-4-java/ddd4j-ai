package io.ddd4j.ai.extension.router.service;

import io.ddd4j.ai.core.AiRequest;
import reactor.core.publisher.Flux;

/**
 * 多模型路由端口：按策略在候选模型池中选择并转发请求。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public interface ChatRouter {

    /**
     * 路由并执行单轮请求，返回选中模型的结果。
     *
     * @param request AI 请求
     * @return 模型回答
     */
    String route(AiRequest request);

    /**
     * 路由并流式执行请求。
     *
     * @param request AI 请求
     * @return 输出内容流
     */
    Flux<String> streamRoute(AiRequest request);
}
