package io.ddd4j.ai.core;

/**
 * 最小化的 AI 调用处理契约：接收请求并返回规范化响应。
 *
 * @author ddd4j
 */
public interface AiHandler extends AiComponent {

    /**
     * 处理一次 AI 调用请求并返回规范化响应。
     *
     * @param request 调用请求，不允许为 {@code null}
     * @return 规范化响应
     */
    AiResponse handle(AiRequest request);
}
