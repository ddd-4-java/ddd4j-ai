package io.ddd4j.ai.extension.tts.service.impl;

import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtime;
import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtimeCallback;
import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtimeConfig;
import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtimeParam;
import com.google.gson.JsonObject;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * {@link DashScopeRealtimeClient} 的 DashScope SDK 实现：包内可见，
 * 通过 {@link DashScopeRealtimeClient#create(String, String, String)} 工厂方法构造。
 *
 * <p>使用示例：
 * <pre>{@code
 * DashScopeRealtimeClient client = DashScopeRealtimeClient.create(
 *         System.getenv("DASHSCOPE_API_KEY"),
 *         "qwen3-tts-12hz-0.6b-customvoice",
 *         "Cherry");
 * client.onEvent(ev -> /* audio delta *\/);
 * client.onClose((code, reason) -> /* log *\/);
 * client.connect();
 * client.appendText("你好");
 * client.commit();
 * client.finish();
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
final class DashScopeSdkClient implements DashScopeRealtimeClient {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DashScopeSdkClient.class);

    private final String apiKey;
    private final String model;
    private final String voice;

    private final AtomicReference<Consumer<JsonObject>> eventHandler = new AtomicReference<>();
    private final AtomicReference<BiConsumer<Integer, String>> closeHandler = new AtomicReference<>();

    private volatile QwenTtsRealtime client;

    DashScopeSdkClient(String apiKey, String model, String voice) {
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
        this.model = Objects.requireNonNull(model, "model");
        this.voice = voice;
    }

    @Override
    public void connect() {
        var paramBuilder = QwenTtsRealtimeParam.builder()
                .model(model)
                .apikey(apiKey);
        QwenTtsRealtimeParam param = paramBuilder.build();

        this.client = new QwenTtsRealtime(param, new QwenTtsRealtimeCallback() {
            @Override
            public void onOpen() {
                // session 建立完成，业务方无需关心
            }

            @Override
            public void onEvent(JsonObject event) {
                Consumer<JsonObject> handler = eventHandler.get();
                if (handler != null) {
                    handler.accept(event);
                }
            }

            @Override
            public void onClose(int code, String reason) {
                BiConsumer<Integer, String> handler = closeHandler.get();
                if (handler != null) {
                    handler.accept(code, reason);
                }
            }
        });

        // 通过 session.update 应用 voice 配置（SDK 在 update 时把 voice 写入 control 帧）
        if (voice != null && !voice.isBlank()) {
            try {
                QwenTtsRealtimeConfig config = QwenTtsRealtimeConfig.builder().voice(voice).build();
                client.updateSession(config);
            } catch (RuntimeException ex) {
                // 旧版本 SDK 可能不暴露 voice builder；记录后继续（用模型默认音色）
                log.warn("DashScope SDK voice 配置失败，使用模型默认: {}", ex.getMessage());
            }
        }

        try {
            client.connect();
        } catch (Exception e) {
            throw new IllegalStateException("DashScope Qwen3-TTS WebSocket 连接失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void appendText(String text) {
        ensureConnected().appendText(text);
    }

    @Override
    public void commit() {
        ensureConnected().commit();
    }

    @Override
    public void finish() {
        ensureConnected().finish();
    }

    @Override
    public void close() {
        QwenTtsRealtime c = this.client;
        if (c != null) {
            c.close();
        }
    }

    @Override
    public void onEvent(Consumer<JsonObject> handler) {
        eventHandler.set(Objects.requireNonNull(handler, "handler"));
    }

    @Override
    public void onClose(BiConsumer<Integer, String> handler) {
        closeHandler.set(Objects.requireNonNull(handler, "handler"));
    }

    private QwenTtsRealtime ensureConnected() {
        QwenTtsRealtime c = client;
        if (c == null) {
            throw new IllegalStateException("DashScope 客户端未连接，请先调用 connect()");
        }
        return c;
    }
}