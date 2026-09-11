package io.ddd4j.ai.extension.tts.service.impl;

import com.google.gson.JsonObject;
import io.ddd4j.ai.extension.tts.metrics.TtsMetrics;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DashScopeRealtimeTtsService} 单元测试：通过 {@link FakeDashScopeRealtimeClient}
 * 控制 SDK 行为，验证流式音频桥接 / TTFA 记录 / 异常传播。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class DashScopeRealtimeTtsServiceTest {

    private static byte[] audioBytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static JsonObject audioDeltaEvent(String base64) {
        JsonObject ev = new JsonObject();
        ev.addProperty("type", "response.audio.delta");
        JsonObject audio = new JsonObject();
        audio.addProperty("delta", base64);
        ev.add("audio", audio);
        return ev;
    }

    private static JsonObject responseDoneEvent() {
        JsonObject ev = new JsonObject();
        ev.addProperty("type", "response.done");
        return ev;
    }

    @Test
    void streamSynthesize_emitsEachAudioChunkAndCompletes() {
        FakeDashScopeRealtimeClient client = new FakeDashScopeRealtimeClient();
        TtsMetrics metrics = new TtsMetrics();
        DashScopeRealtimeTtsService service = new DashScopeRealtimeTtsService(client, metrics, "Cherry");

        byte[] first = audioBytes("hello ");
        byte[] second = audioBytes("world");

        // 订阅后立即推送两个 delta + done
        Flux<byte[]> audio = service.streamSynthesize("hello world", "Cherry")
                .doOnSubscribe(s -> client.deliver(audioDeltaEvent(Base64.getEncoder().encodeToString(first))))
                .doOnSubscribe(s -> client.deliver(audioDeltaEvent(Base64.getEncoder().encodeToString(second))))
                .doOnSubscribe(s -> client.deliver(responseDoneEvent()));

        StepVerifier.create(audio)
                .expectNextMatches(arr -> java.util.Arrays.equals(arr, first))
                .expectNextMatches(arr -> java.util.Arrays.equals(arr, second))
                .expectComplete()
                .verify();

        // TTFA 应被记录（一次）
        assertThat(metrics.sampleCount()).isEqualTo(1);
        // 驱动序列正确
        assertThat(client.connectCalls.get()).isEqualTo(1);
        assertThat(client.appendedTexts).containsExactly("hello world");
        assertThat(client.commitCalls.get()).isEqualTo(1);
        assertThat(client.finishCalls.get()).isEqualTo(1);
    }

    @Test
    void streamSynthesize_recordsTtfaOnce() {
        FakeDashScopeRealtimeClient client = new FakeDashScopeRealtimeClient();
        TtsMetrics metrics = new TtsMetrics();
        DashScopeRealtimeTtsService service = new DashScopeRealtimeTtsService(client, metrics, "Cherry");

        byte[] a = audioBytes("a");
        byte[] b = audioBytes("b");
        Flux<byte[]> audio = service.streamSynthesize("ab", "Cherry")
                .doOnSubscribe(s -> client.deliver(audioDeltaEvent(Base64.getEncoder().encodeToString(a))))
                .doOnSubscribe(s -> client.deliver(audioDeltaEvent(Base64.getEncoder().encodeToString(b))))
                .doOnSubscribe(s -> client.deliver(responseDoneEvent()));

        StepVerifier.create(audio).expectNextCount(2).expectComplete().verify();

        // 多次 audio delta 仍只产生 1 次 TTFA 采样
        assertThat(metrics.sampleCount()).isEqualTo(1);
    }

    @Test
    void streamSynthesize_usesDefaultVoiceWhenVoiceBlank() {
        FakeDashScopeRealtimeClient client = new FakeDashScopeRealtimeClient();
        TtsMetrics metrics = new TtsMetrics();
        DashScopeRealtimeTtsService service = new DashScopeRealtimeTtsService(client, metrics, "Cherry");

        byte[] a = audioBytes("a");
        Flux<byte[]> audio = service.streamSynthesize("hi", null)
                .doOnSubscribe(s -> client.deliver(audioDeltaEvent(Base64.getEncoder().encodeToString(a))))
                .doOnSubscribe(s -> client.deliver(responseDoneEvent()));

        StepVerifier.create(audio).expectNextMatches(arr -> java.util.Arrays.equals(arr, a)).expectComplete().verify();

        assertThat(service.getDefaultVoice()).isEqualTo("Cherry");
    }

    @Test
    void streamSynthesize_ignoresNonAudioEvents() {
        FakeDashScopeRealtimeClient client = new FakeDashScopeRealtimeClient();
        TtsMetrics metrics = new TtsMetrics();
        DashScopeRealtimeTtsService service = new DashScopeRealtimeTtsService(client, metrics, "Cherry");

        byte[] a = audioBytes("a");
        JsonObject sessionCreated = new JsonObject();
        sessionCreated.addProperty("type", "session.created");

        Flux<byte[]> audio = service.streamSynthesize("hi", "Cherry")
                .doOnSubscribe(s -> client.deliver(sessionCreated))
                .doOnSubscribe(s -> client.deliver(audioDeltaEvent(Base64.getEncoder().encodeToString(a))))
                .doOnSubscribe(s -> client.deliver(responseDoneEvent()));

        StepVerifier.create(audio).expectNextMatches(arr -> java.util.Arrays.equals(arr, a)).expectComplete().verify();
        assertThat(metrics.sampleCount()).isEqualTo(1);
    }

    @Test
    void synthesize_aggregatesAllBytes() throws Exception {
        FakeDashScopeRealtimeClient client = new FakeDashScopeRealtimeClient();
        TtsMetrics metrics = new TtsMetrics();
        DashScopeRealtimeTtsService service = new DashScopeRealtimeTtsService(client, metrics, "Cherry");

        byte[] a = audioBytes("foo");
        byte[] b = audioBytes("bar");

        // 在独立线程推送 delta，避免订阅竞态
        Thread pusher = new Thread(() -> {
            try {
                Thread.sleep(50);
                client.deliver(audioDeltaEvent(Base64.getEncoder().encodeToString(a)));
                client.deliver(audioDeltaEvent(Base64.getEncoder().encodeToString(b)));
                client.deliver(responseDoneEvent());
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        });
        pusher.start();

        byte[] result = service.synthesize("foobar", "Cherry");
        pusher.join();

        assertThat(new String(result, StandardCharsets.UTF_8)).isEqualTo("foobar");
        assertThat(metrics.sampleCount()).isEqualTo(1);
    }

    @Test
    void streamSynthesize_propagatesCloseEvent() {
        FakeDashScopeRealtimeClient client = new FakeDashScopeRealtimeClient();
        TtsMetrics metrics = new TtsMetrics();
        DashScopeRealtimeTtsService service = new DashScopeRealtimeTtsService(client, metrics, "Cherry");

        Flux<byte[]> audio = service.streamSynthesize("hi", "Cherry")
                .doOnSubscribe(s -> client.deliverClose(1000, "normal"));

        StepVerifier.create(audio).expectComplete().verify();
    }

    /**
     * 假的 DashScope 客户端：保留事件回调，让测试通过 {@link #deliver(JsonObject)}
     * 主动推送音频 delta，验证服务正确桥接到 Sink。
     */
    static final class FakeDashScopeRealtimeClient implements DashScopeRealtimeClient {

        private final AtomicInteger connectCalls = new AtomicInteger();
        final List<String> appendedTexts = new CopyOnWriteArrayList<>();
        private final AtomicInteger commitCalls = new AtomicInteger();
        private final AtomicInteger finishCalls = new AtomicInteger();
        private final AtomicInteger closeCalls = new AtomicInteger();

        private Consumer<JsonObject> eventHandler;
        private BiConsumer<Integer, String> closeHandler;

        @Override
        public void connect() {
            connectCalls.incrementAndGet();
        }

        @Override
        public void appendText(String text) {
            appendedTexts.add(text);
        }

        @Override
        public void commit() {
            commitCalls.incrementAndGet();
        }

        @Override
        public void finish() {
            finishCalls.incrementAndGet();
        }

        @Override
        public void close() {
            closeCalls.incrementAndGet();
        }

        @Override
        public void onEvent(Consumer<JsonObject> handler) {
            this.eventHandler = handler;
        }

        @Override
        public void onClose(BiConsumer<Integer, String> handler) {
            this.closeHandler = handler;
        }

        void deliver(JsonObject ev) {
            if (eventHandler != null) {
                eventHandler.accept(ev);
            }
        }

        void deliverClose(int code, String reason) {
            if (closeHandler != null) {
                closeHandler.accept(code, reason);
            }
        }
    }
}