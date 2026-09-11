package io.ddd4j.ai.extension.tts.router;

import io.ddd4j.ai.extension.tts.service.TtsService;
import reactor.core.publisher.Flux;

/**
 * TTS 多后端路由器：在多后端链路上按 primary → fallback 顺序尝试合成，
 * 主后端失败时自动降级到下一个，业务方只面对一个统一入口。
 *
 * <p>设计对偶于 ddd4j-ai 的 {@code MultiModelChatRouter}（router 扩展）——
 * 不同之处在于：Router 是按"主备降级"而非"轮询加权"选择后端。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public interface TtsRouter extends TtsService {

    /**
     * 链路上注册的所有后端（按优先级排序：primary 在前）。
     *
     * @return 后端列表（不可变）
     */
    java.util.List<String> backendNames();
}