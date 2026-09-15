package io.ddd4j.ai.extension.tts.service.impl;

import com.google.gson.JsonObject;
import io.ddd4j.ai.extension.tts.metrics.TtsMetrics;
import io.ddd4j.ai.extension.tts.service.TtsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.concurrent.Queues;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DashScope Qwen3-TTS Realtime 流式后端：通过 WebSocket 长连接把 LLM 流式吐出的文本送入
 * Qwen3-TTS，立刻返回音频字节流（首块 TTFA 业界领先 ~97ms，参考 agentscope-cpp
 * {@code docs/superpowers/plans/2026-09-10-tts-tech-decision.md}）。
 *
 * <p>核心设计点（借鉴自 agentscope-cpp）：
 * <ul>
 *   <li><b>TTFA 优先</b>：自 {@link DashScopeRealtimeClient#connect()} 起算，到首个 audio delta 为止记录耗时。</li>
 *   <li><b>流式输入流式输出</b>：{@link DashScopeRealtimeClient#appendText(String)} 多次追加 →
 *       {@link DashScopeRealtimeClient#commit()} 提交 → {@link DashScopeRealtimeClient#finish()} 标记结束。</li>
 *   <li><b>Sink + boundedElastic</b>：通过 {@code Sinks.Many<byte[]>} 把回调里的字节桥接到 Reactor 流。</li>
 * </ul>
 *
 * <p>{@link #synthesize(String, String)} 单元素版聚合 {@link #streamSynthesize(String, String)} 的所有字节，
 * 保留向后兼容。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class DashScopeRealtimeTtsService implements TtsService {

    private static final Logger log = LoggerFactory.getLogger(DashScopeRealtimeTtsService.class);

    private final DashScopeRealtimeClient client;
    private final TtsMetrics metrics;
    private final String defaultVoice;

    public DashScopeRealtimeTtsService(DashScopeRealtimeClient client,
                                       TtsMetrics metrics,
                                       String defaultVoice) {
        this.client = Objects.requireNonNull(client, "client");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        this.defaultVoice = defaultVoice;
    }

    /** 工厂方法：构造时直接创建 DashScope SDK 客户端（用于 AutoConfiguration）。 */
    public static DashScopeRealtimeTtsService create(String apiKey,
                                                      String model,
                                                      String voice,
                                                      TtsMetrics metrics) {
        DashScopeRealtimeClient client = DashScopeRealtimeClient.create(apiKey, model, voice);
        return new DashScopeRealtimeTtsService(client, metrics, voice);
    }

    @Override
    public byte[] synthesize(String text, String voice) throws Exception {
        // 聚合所有流式字节后返回（与原 TtsService 阻塞调用保持一致）
        byte[][] holder = new byte[1][];
        holder[0] = new byte[0];
        streamSynthesize(text, voice)
                .doOnNext(chunk -> {
                    byte[] prev = holder[0];
                    byte[] next = new byte[prev.length + chunk.length];
                    System.arraycopy(prev, 0, next, 0, prev.length);
                    System.arraycopy(chunk, 0, next, prev.length, chunk.length);
                    holder[0] = next;
                })
                .blockLast(java.time.Duration.ofSeconds(60));
        return holder[0];
    }

    @Override
    public Flux<byte[]> streamSynthesize(String text, String voice) {
        String effectiveVoice = (voice == null || voice.isBlank()) ? defaultVoice : voice;
        String effectiveText = text == null ? "" : text;

        // 单播 + 缓冲，避免订阅前到达的音频字节丢失
        reactor.core.publisher.Sinks.Many<byte[]> sink =
                reactor.core.publisher.Sinks.many().unicast().onBackpressureBuffer(Queues.<byte[]>unbounded().get());
        AtomicLong startNanos = new AtomicLong(System.nanoTime());
        AtomicLong ttfaRecorded = new AtomicLong(0);

        client.onEvent(event -> {
            try {
                // response.done：合成结束信号，完成音频流（在 audio 提取之前判断）
                if (event != null && event.has("type")
                        && "response.done".equals(event.get("type").getAsString())) {
                    sink.tryEmitComplete();
                    return;
                }
                JsonObject payload = extractAudioPayload(event);
                if (payload == null) {
                    return;
                }
                String base64 = payload.get("delta").getAsString();
                byte[] audio = Base64.getDecoder().decode(base64);
                if (ttfaRecorded.compareAndSet(0, 1)) {
                    long elapsed = System.nanoTime() - startNanos.get();
                    metrics.recordTtfa(elapsed);
                    log.debug("tts.firstaudio: {}ms ({} bytes)", elapsed / 1_000_000L, audio.length);
                }
                sink.tryEmitNext(audio);
            } catch (RuntimeException ex) {
                log.warn("DashScope 音频解码失败: {}", ex.getMessage());
            }
        });

        client.onClose((code, reason) -> {
            log.debug("DashScope WebSocket closed: code={}, reason={}", code, reason);
            sink.tryEmitComplete();
        });

        // 异步驱动：connect → appendText → commit → finish
        Mono.fromRunnable(() -> {
                    try {
                        client.connect();
                        if (!effectiveText.isEmpty()) {
                            client.appendText(effectiveText);
                            client.commit();
                        }
                        client.finish();
                    } catch (RuntimeException ex) {
                        sink.tryEmitError(ex);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        v -> { /* no-op: 驱动侧不产出值 */ },
                        err -> {
                            // sink.tryEmitError 可能因 onClose 已先完成而失败（EmitResult.FAIL_TERMINATED），
                            // 此处兜底确保异常至少被 log；避免 Reactor Hooks.onErrorDropped 静默吞掉。
                            log.warn("DashScope TTS 驱动链异常（sink 可能已完成）: {}", err.getMessage());
                        }
                );

        return sink.asFlux()
                .doOnCancel(client::close)
                .doOnError(err -> {
                    log.warn("DashScope TTS 流式失败: {}", err.getMessage());
                    client.close();
                });
    }

    /**
     * 从 DashScope 事件 JSON 中提取 audio.delta。
     *
     * <p>协议约定（参考 agentscope-cpp 第 5.2 节）：
     * <pre>
     * Server → Client:
     *   session.created
     *   response.audio.delta {delta: <base64 audio>}
     *   response.audio.done
     *   response.done
     * </pre>
     */
    private static JsonObject extractAudioPayload(JsonObject event) {
        if (event == null) {
            return null;
        }
        // 形态 1: {type: "response.audio.delta", audio: {delta: "..."}}
        if (event.has("audio") && event.get("audio").isJsonObject()) {
            JsonObject audio = event.getAsJsonObject("audio");
            if (audio.has("delta")) {
                return audio;
            }
        }
        // 形态 2: 直接 {delta: "..."} 或 {type, delta}
        if (event.has("delta") && event.get("delta").isJsonPrimitive()
                && event.get("delta").getAsJsonPrimitive().isString()) {
            JsonObject wrapped = new JsonObject();
            wrapped.add("delta", event.get("delta"));
            return wrapped;
        }
        return null;
    }

    /** 当前 TTS 指标快照（用于运维 / 测试断言）。 */
    public TtsMetrics getMetrics() {
        return metrics;
    }

    /** 释放底层 WebSocket 连接（业务方按需调用；spring 关闭时会自动 close）。 */
    public void close() {
        client.close();
    }

    /** 默认音色的 getter（用于配置同步 / 测试断言）。 */
    public String getDefaultVoice() {
        return defaultVoice;
    }
}