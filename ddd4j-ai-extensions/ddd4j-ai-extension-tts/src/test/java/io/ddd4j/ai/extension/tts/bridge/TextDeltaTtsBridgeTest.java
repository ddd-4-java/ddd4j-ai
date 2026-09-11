package io.ddd4j.ai.extension.tts.bridge;

import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.TextBlockEndEvent;
import io.ddd4j.ai.extension.tts.chunk.TextChunker;
import io.ddd4j.ai.extension.tts.service.TtsService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link TextDeltaTtsBridge} 集成测试：用 Mockito mock AgentEvent 与 TtsService，
 * 验证流式切片 → TTS 调用 → 音频拼接的正确性。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class TextDeltaTtsBridgeTest {

    private static TextBlockDeltaEvent mockDelta(String delta) {
        TextBlockDeltaEvent ev = mock(TextBlockDeltaEvent.class);
        when(ev.getDelta()).thenReturn(delta);
        return ev;
    }

    private static TextBlockEndEvent mockEnd() {
        return mock(TextBlockEndEvent.class);
    }

    private static AgentEvent mockOther() {
        return mock(AgentEvent.class);
    }

    private static byte[] audio(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void pipesTextDeltaToTtsAndReturnsAudio() {
        TextChunker chunker = TextChunker.defaultChunker();
        TtsService tts = mock(TtsService.class);
        when(tts.streamSynthesize(eq("你好世界！"), any()))
                .thenReturn(Flux.just(audio("audio-bytes")));

        TextDeltaTtsBridge bridge = new TextDeltaTtsBridge(chunker, tts);

        Flux<AgentEvent> events = Flux.just(
                mockDelta("你"),
                mockDelta("好"),
                mockDelta("世"),
                mockDelta("界"),
                mockDelta("！"),
                mockEnd());

        StepVerifier.create(bridge.pipe(events, null))
                .expectNextMatches(arr -> java.util.Arrays.equals(arr, audio("audio-bytes")))
                .expectComplete()
                .verify();

        verify(tts, atLeastOnce()).streamSynthesize(eq("你好世界！"), eq(null));
    }

    @Test
    void splitsMultipleSentencesIntoSeparateTtsCalls() {
        TextChunker chunker = TextChunker.defaultChunker();
        TtsService tts = mock(TtsService.class);

        // 第一个分句命中句末标点立即送 TTS
        when(tts.streamSynthesize(eq("你好。"), any())).thenReturn(Flux.just(audio("a1")));
        // 第二个分句在 flush 时送 TTS（end 事件前 chunker 还在累积）
        when(tts.streamSynthesize(eq("世界。"), any())).thenReturn(Flux.just(audio("a2")));

        TextDeltaTtsBridge bridge = new TextDeltaTtsBridge(chunker, tts);

        Flux<AgentEvent> events = Flux.just(
                mockDelta("你好。"),
                mockDelta("世"),
                mockDelta("界"),
                mockDelta("。"),
                mockEnd());

        StepVerifier.create(bridge.pipe(events, "Cherry"))
                .expectNextMatches(arr -> java.util.Arrays.equals(arr, audio("a1")))
                .expectNextMatches(arr -> java.util.Arrays.equals(arr, audio("a2")))
                .expectComplete()
                .verify();
    }

    @Test
    void ignoresNonTextEvents() {
        TextChunker chunker = TextChunker.defaultChunker();
        TtsService tts = mock(TtsService.class);
        when(tts.streamSynthesize(any(), any()))
                .thenAnswer(inv -> Flux.just(audio("chunk")));

        TextDeltaTtsBridge bridge = new TextDeltaTtsBridge(chunker, tts);

        Flux<AgentEvent> events = Flux.just(
                mockOther(),
                mockDelta("hi"),
                mockEnd());

        StepVerifier.create(bridge.pipe(events, null))
                .expectNextCount(1)
                .expectComplete()
                .verify();
    }

    @Test
    void handlesEmptyDelta() {
        TextChunker chunker = TextChunker.defaultChunker();
        TtsService tts = mock(TtsService.class);
        when(tts.streamSynthesize(any(), any()))
                .thenReturn(Flux.just(audio("chunk")));

        TextDeltaTtsBridge bridge = new TextDeltaTtsBridge(chunker, tts);

        Flux<AgentEvent> events = Flux.just(
                mockDelta(""),
                mockDelta("hi"),
                mockEnd());

        StepVerifier.create(bridge.pipe(events, null))
                .expectNextCount(1)
                .expectComplete()
                .verify();
    }

    @Test
    void agentStreamCompleteWithoutEndEvent_flushesRemaining() {
        TextChunker chunker = TextChunker.defaultChunker();
        TtsService tts = mock(TtsService.class);
        when(tts.streamSynthesize(eq("unterminated"), any()))
                .thenReturn(Flux.just(audio("tail")));

        TextDeltaTtsBridge bridge = new TextDeltaTtsBridge(chunker, tts);

        Flux<AgentEvent> events = Flux.just(mockDelta("unterminated"));

        StepVerifier.create(bridge.pipe(events, null))
                .expectNextMatches(arr -> java.util.Arrays.equals(arr, audio("tail")))
                .expectComplete()
                .verify();
    }

    @Test
    void synthesizeDelegatesDirectly() {
        TtsService tts = mock(TtsService.class);
        when(tts.streamSynthesize(eq("text"), eq("Cherry")))
                .thenReturn(Flux.just(audio("a")));

        TextDeltaTtsBridge bridge = new TextDeltaTtsBridge(TextChunker.defaultChunker(), tts);

        StepVerifier.create(bridge.synthesize("text", "Cherry"))
                .expectNextMatches(arr -> java.util.Arrays.equals(arr, audio("a")))
                .expectComplete()
                .verify();
    }

    @Test
    void resetClearsPendingBuffer() {
        TextChunker chunker = TextChunker.defaultChunker();
        chunker.feed("abc");
        TextDeltaTtsBridge bridge = new TextDeltaTtsBridge(chunker, mock(TtsService.class));

        assertThat(bridge.pendingChunks()).isEqualTo(3);
        bridge.reset();
        assertThat(bridge.pendingChunks()).isZero();
    }

    @Test
    void ttsInvocationHistory_recordsAllChunks() {
        TextChunker chunker = TextChunker.defaultChunker();
        TtsService tts = mock(TtsService.class);
        List<String> captured = new ArrayList<>();
        when(tts.streamSynthesize(any(), any())).thenAnswer(inv -> {
            captured.add(inv.getArgument(0));
            return Flux.just(audio("ok"));
        });

        TextDeltaTtsBridge bridge = new TextDeltaTtsBridge(chunker, tts);

        Flux<AgentEvent> events = Flux.just(
                mockDelta("一"),
                mockDelta("二"),
                mockDelta("。"),
                mockDelta("三"),
                mockDelta("。"),
                mockEnd());

        StepVerifier.create(bridge.pipe(events, null))
                .expectNextCount(2)
                .expectComplete()
                .verify();

        assertThat(captured).containsExactly("一二。", "三。");
    }

    @Test
    void pipe_realTextBlockDeltaEvent_feedsChunkerCorrectly() {
        // 用真实 TextBlockDeltaEvent（非 Mockito mock）验证该类型的 getDelta()
        // 与 TextChunker 的期望一致——mock 版只能断言我们对它的假定。
        // 3 参构造器语义已由 javap -c 的 putfield 顺序确认：(replyId, blockId, delta)
        TextChunker chunker = TextChunker.defaultChunker();
        TtsService tts = mock(TtsService.class);
        when(tts.streamSynthesize(eq("你好。"), any()))
                .thenReturn(Flux.just(audio("real-1")));

        TextDeltaTtsBridge bridge = new TextDeltaTtsBridge(chunker, tts);

        // 句末标点触发 feed() 切分；流自然结束走 doOnComplete 的 flush 分支
        // （不使用 TextBlockEndEvent——其构造器未确认）
        Flux<AgentEvent> events = Flux.just(
                new TextBlockDeltaEvent("r1", "b1", "你好。"));

        StepVerifier.create(bridge.pipe(events, null))
                .expectNextMatches(arr -> java.util.Arrays.equals(arr, audio("real-1")))
                .expectComplete()
                .verify();

        verify(tts, atLeastOnce()).streamSynthesize(eq("你好。"), eq(null));
    }
}