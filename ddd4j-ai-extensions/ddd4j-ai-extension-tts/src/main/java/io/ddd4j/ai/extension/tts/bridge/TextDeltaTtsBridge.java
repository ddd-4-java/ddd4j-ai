package io.ddd4j.ai.extension.tts.bridge;

import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.TextBlockEndEvent;
import io.ddd4j.ai.extension.tts.chunk.TextChunker;
import io.ddd4j.ai.extension.tts.service.TtsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.concurrent.Queues;

import java.util.Objects;

/**
 * Agent 文本增量流 → TTS 音频流的桥接器：借鉴自 agentscope-cpp 的 StreamFirstChunkMiddleware。
 *
 * <p>典型用法（业务方组合，无需自动装配）：
 * <pre>{@code
 * TextDeltaTtsBridge bridge = new TextDeltaTtsBridge(
 *         TextChunker.create(24, 1), ttsRouter);
 * Flux<byte[]> audio = bridge.pipe(
 *         agentService.streamSynthesizeEvents(instruction),
 *         "zh-CN-XiaoxiaoNeural");
 * }</pre>
 *
 * <p>工作流程：
 * <ol>
 *   <li>订阅 {@link AgentEvent} 流，识别 {@link TextBlockDeltaEvent} → 调用
 *       {@link TextChunker#feed(String)}；切分出的 chunks 立即送 TTS。</li>
 *   <li>收到 {@link TextBlockEndEvent} → {@link TextChunker#flush()} 残余文本 → 送 TTS，
 *       然后完成整体音频流。</li>
 *   <li>其他事件类型（工具调用、思考块等）忽略。</li>
 * </ol>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class TextDeltaTtsBridge {

    private static final Logger log = LoggerFactory.getLogger(TextDeltaTtsBridge.class);

    private final TextChunker chunker;
    private final TtsService tts;

    public TextDeltaTtsBridge(TextChunker chunker, TtsService tts) {
        this.chunker = Objects.requireNonNull(chunker, "chunker");
        this.tts = Objects.requireNonNull(tts, "tts");
    }

    /** 便捷工厂：使用默认 chunker (24/1)。 */
    public static TextDeltaTtsBridge withDefaults(TtsService tts) {
        return new TextDeltaTtsBridge(TextChunker.defaultChunker(), tts);
    }

    /**
     * 订阅 agent 事件流，把文本 delta 流式送入 TTS，返回拼接后的音频字节流。
     *
     * <p>音频流的元素是 TTS 后端返回的 {@code byte[]}（多个 chunk 按顺序拼接）。
     * 当 agent 流以 {@link TextBlockEndEvent} 结束时，整个音频流正常结束；
     * 若 agent 流中途出错或被取消，音频流也会相应传播错误 / 取消。
     *
     * @param agentEvents Agentscope {@code HarnessAgent.streamEvents(...)} 返回的事件流
     * @param voice       TTS 音色（null 用 TTS 默认音色）
     * @return 音频字节流（{@link Flux<byte[]>}）
     */
    public Flux<byte[]> pipe(Flux<AgentEvent> agentEvents, String voice) {
        Objects.requireNonNull(agentEvents, "agentEvents");
        reactor.core.publisher.Sinks.Many<byte[]> sink =
                reactor.core.publisher.Sinks.many().unicast().onBackpressureBuffer(Queues.<byte[]>unbounded().get());

        agentEvents
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(event -> {
                    try {
                        if (event instanceof TextBlockDeltaEvent delta) {
                            String text = delta.getDelta();
                            for (String chunk : chunker.feed(text)) {
                                tts.streamSynthesize(chunk, voice)
                                        .doOnNext(sink::tryEmitNext)
                                        .doOnError(err -> sink.tryEmitError(err))
                                        .subscribe();
                            }
                        } else if (event instanceof TextBlockEndEvent) {
                            String tail = chunker.flush();
                            if (!tail.isEmpty()) {
                                tts.streamSynthesize(tail, voice)
                                        .doOnNext(sink::tryEmitNext)
                                        .doOnError(err -> sink.tryEmitError(err))
                                        .subscribe();
                            }
                            sink.tryEmitComplete();
                        }
                        // 其他事件（thinking/tool/hint...）暂忽略，避免 thinking 触发 TTS
                    } catch (RuntimeException ex) {
                        log.warn("TTS 桥接处理事件失败: {}", ex.getMessage());
                        sink.tryEmitError(ex);
                    }
                })
                .doOnError(err -> sink.tryEmitError(err))
                .doOnComplete(() -> {
                    // agent 流结束但没有收到 TextBlockEndEvent（罕见，例如空响应）也触发一次 flush
                    String tail = chunker.flush();
                    if (!tail.isEmpty()) {
                        tts.streamSynthesize(tail, voice)
                                .doOnNext(sink::tryEmitNext)
                                .subscribe();
                    }
                    sink.tryEmitComplete();
                })
                .subscribe();

        return sink.asFlux();
    }

    /**
     * 单段文本版（不订阅事件流，直接把文本送 TTS）—— 主要用于测试与短文本场景。
     */
    public Flux<byte[]> synthesize(String text, String voice) {
        return tts.streamSynthesize(text, voice);
    }

    /** 重置分块器（每轮新对话前调用）。 */
    public void reset() {
        chunker.reset();
    }

    /** 当前 chunker 缓冲长度（用于调试）。 */
    public int pendingChunks() {
        return chunker.pendingSize();
    }

    /** 抑制未使用警告（Mono 类型仅供后续扩展使用）。 */
    @SuppressWarnings("unused")
    private static <T> Mono<T> ignored() {
        return Mono.empty();
    }
}