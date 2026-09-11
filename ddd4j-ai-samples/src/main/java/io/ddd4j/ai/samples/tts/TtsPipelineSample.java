package io.ddd4j.ai.samples.tts;

import io.ddd4j.ai.extension.tts.bridge.TextDeltaTtsBridge;
import io.ddd4j.ai.extension.tts.chunk.TextChunker;
import io.ddd4j.ai.extension.tts.metrics.TtsMetrics;
import io.ddd4j.ai.extension.tts.router.TtsRouter;
import io.ddd4j.ai.extension.tts.service.TtsService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * TTS 高性能管道示例：演示如何在业务方代码里使用新版 TTS 组件。
 *
 * <p>覆盖三个用法：
 * <ol>
 *   <li>{@link #streamSpeech(String, String)} — 直接流式合成（DashScope 真正流式，Edge 单元素流）</li>
 *   <li>{@link #pipeAgentTextDeltas(Flux, String)} — 与 Agent 事件流对接（TextDeltaTtsBridge）</li>
 *   <li>{@link #synthesizeAndLogMetrics(String, String)} — 单次合成 + 输出当前 TTFA 统计</li>
 * </ol>
 *
 * <p>注意：本示例展示用法而非真实联机，{@link #streamSpeech(String, String)} 在 DashScope 后端
 * 启用时通过 WebSocket 流式输出音频帧；在 Edge 后端时为单元素 mp3 流。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Component
public class TtsPipelineSample {

    private final TtsRouter ttsRouter;
    private final TtsService tts;
    private final TtsMetrics metrics;

    public TtsPipelineSample(TtsRouter ttsRouter, TtsService tts, TtsMetrics metrics) {
        this.ttsRouter = ttsRouter;
        this.tts = tts;
        this.metrics = metrics;
    }

    /**
     * 流式合成：业务方直接订阅返回的 Flux 即可拿到音频字节流。
     */
    public Flux<byte[]> streamSpeech(String text, String voice) {
        return ttsRouter.streamSynthesize(text, voice);
    }

    /**
     * 与 Agent 文本增量事件流对接：把 LLM 流式吐出的文本送 TTS 立即合成。
     *
     * <p>典型用法（需要 {@code ddd4j-ai-extension-agent} 在 classpath）：
     * <pre>{@code
     * Flux<byte[]> audio = sample.pipeAgentTextDeltas(
     *         agentService.streamSynthesizeEvents(instruction), "zh-CN-XiaoxiaoNeural");
     * audio.subscribe(bytes -> writeToPlayer(bytes));
     * }</pre>
     */
    public Flux<byte[]> pipeAgentTextDeltas(Flux<io.agentscope.core.event.AgentEvent> agentEvents,
                                            String voice) {
        TextDeltaTtsBridge bridge = new TextDeltaTtsBridge(
                TextChunker.create(24, 1), tts);
        return bridge.pipe(agentEvents, voice);
    }

    /**
     * 单次合成 + 摘要当前 TTFA 指标：用于演示日志输出。
     */
    public byte[] synthesizeAndLogMetrics(String text, String voice) throws Exception {
        byte[] audio = tts.synthesize(text, voice);
        metrics.logSummary(org.slf4j.LoggerFactory.getLogger(TtsPipelineSample.class));
        return audio;
    }
}