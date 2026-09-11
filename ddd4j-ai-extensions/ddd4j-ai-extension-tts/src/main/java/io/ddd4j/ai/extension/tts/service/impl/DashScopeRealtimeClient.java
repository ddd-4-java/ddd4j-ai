package io.ddd4j.ai.extension.tts.service.impl;

import com.google.gson.JsonObject;

import java.util.function.Consumer;

/**
 * DashScope Qwen3-TTS Realtime API 客户端抽象：把
 * {@code com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtime} 的 okhttp WebSocket
 * 交互抽象为 {@link #connect() / #appendText(String) / #commit() / #finish() / #close()}，
 * 通过 {@link Consumer} 回调把音频字节回调到上层。
 *
 * <p>引入此抽象的原因：
 * <ul>
 *   <li>业务方在测试中可注入 mock 实现，无需拉起真实 WebSocket</li>
 *   <li>把"逐块推送字节"语义与 DashScope SDK 的 session/event 协议解耦</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public interface DashScopeRealtimeClient {

    /** 启动 WebSocket 长连接（dashscope SDK 内部异步建链）。 */
    void connect();

    /** 流式追加文本片段（与 Qwen3-TTS 的 input_text_delta 对齐）。 */
    void appendText(String text);

    /** 提交当前累积的文本缓冲（与 Qwen3-TTS 的 input_text_done 对齐）。 */
    void commit();

    /** 标记流结束，等待最后音频（与 Qwen3-TTS 的 response.done 对齐）。 */
    void finish();

    /** 强制关闭 WebSocket。 */
    void close();

    /** 接收音频/事件回调（按 SDK 推送顺序）。 */
    void onEvent(Consumer<JsonObject> handler);

    /** 接收关闭事件回调（code + reason）。 */
    void onClose(java.util.function.BiConsumer<Integer, String> handler);

    /**
     * 工厂方法：构造一个真正调用 DashScope SDK 的客户端实现。
     *
     * @param apiKey   DashScope 控制台申请的 API key
     * @param model    TTS 模型名（如 {@code qwen3-tts-12hz-0.6b-customvoice}）
     * @param voice    音色 shortName（如 {@code Cherry}），null 用模型默认
     */
    static DashScopeRealtimeClient create(String apiKey, String model, String voice) {
        return new DashScopeSdkClient(apiKey, model, voice);
    }
}