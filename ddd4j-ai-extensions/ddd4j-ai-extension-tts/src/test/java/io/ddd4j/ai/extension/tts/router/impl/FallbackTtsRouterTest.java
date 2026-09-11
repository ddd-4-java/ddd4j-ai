package io.ddd4j.ai.extension.tts.router.impl;

import io.ddd4j.ai.extension.tts.router.TtsRouter;
import io.ddd4j.ai.extension.tts.service.TtsService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link FallbackTtsRouter} 降级切换测试。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class FallbackTtsRouterTest {

    private static byte[] audio(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void rejectsEmptyBackendChain() {
        assertThatThrownBy(() -> new FallbackTtsRouter(Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("backends must not be empty");
    }

    @Test
    void synthesize_primarySuccess_returnsPrimaryBytes() throws Exception {
        TtsService primary = fake("primary", audio("hello"));
        TtsService fallback = fake("fallback", audio("hello-fb"));
        FallbackTtsRouter router = new FallbackTtsRouter(ordered("primary", primary, "fallback", fallback));

        byte[] result = router.synthesize("hi", null);
        assertThat(new String(result, StandardCharsets.UTF_8)).isEqualTo("hello");
    }

    @Test
    void synthesize_primaryFailsFallsBack() throws Exception {
        TtsService primary = failing("primary", new IllegalStateException("network down"));
        TtsService fallback = fake("fallback", audio("hello-fb"));
        FallbackTtsRouter router = new FallbackTtsRouter(ordered("primary", primary, "fallback", fallback));

        byte[] result = router.synthesize("hi", null);
        assertThat(new String(result, StandardCharsets.UTF_8)).isEqualTo("hello-fb");
    }

    @Test
    void synthesize_allBackendsFail_throwsAggregate() {
        TtsService primary = failing("primary", new IllegalStateException("p"));
        TtsService fallback = failing("fallback", new RuntimeException("f"));
        FallbackTtsRouter router = new FallbackTtsRouter(ordered("primary", primary, "fallback", fallback));

        assertThatThrownBy(() -> router.synthesize("hi", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("所有 TTS 后端均不可用")
                .satisfies(t -> assertThat(t.getSuppressed()).hasSize(2));
    }

    @Test
    void streamSynthesize_primarySuccess_returnsPrimaryFlux() {
        TtsService primary = fakeStream("primary", List.of(audio("a"), audio("b")));
        TtsService fallback = fakeStream("fallback", List.of(audio("fb")));
        TtsRouter router = new FallbackTtsRouter(ordered("primary", primary, "fallback", fallback));

        StepVerifier.create(router.streamSynthesize("hi", null))
                .expectNextMatches(arr -> java.util.Arrays.equals(arr, audio("a")))
                .expectNextMatches(arr -> java.util.Arrays.equals(arr, audio("b")))
                .expectComplete()
                .verify();
    }

    @Test
    void streamSynthesize_primaryFailsBeforeFirstChunk_fallsBack() {
        TtsService primary = failingStream("primary", new IllegalStateException("ws closed"));
        TtsService fallback = fakeStream("fallback", List.of(audio("fb")));
        TtsRouter router = new FallbackTtsRouter(ordered("primary", primary, "fallback", fallback));

        StepVerifier.create(router.streamSynthesize("hi", null))
                .expectNextMatches(arr -> java.util.Arrays.equals(arr, audio("fb")))
                .expectComplete()
                .verify();
    }

    @Test
    void streamSynthesize_allBackendsFail_emitsAggregateError() {
        TtsService primary = failingStream("primary", new IllegalStateException("p"));
        TtsService fallback = failingStream("fallback", new RuntimeException("f"));
        TtsRouter router = new FallbackTtsRouter(ordered("primary", primary, "fallback", fallback));

        StepVerifier.create(router.streamSynthesize("hi", null))
                .expectErrorSatisfies(err -> {
                    assertThat(err).isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("所有 TTS 后端均不可用");
                    assertThat(err.getSuppressed()).hasSize(2);
                })
                .verify();
    }

    @Test
    void streamSynthesize_primaryPartialStreamDoesNotFallBackMidStream() {
        // 流已经开始就不能降级（避免音频格式混杂）—— v1 行为：中途错误直接传播
        TtsService primary = partialStream("primary", List.of(audio("a")), new IllegalStateException("mid-stream error"));
        TtsService fallback = fakeStream("fallback", List.of(audio("fb")));
        TtsRouter router = new FallbackTtsRouter(ordered("primary", primary, "fallback", fallback));

        StepVerifier.create(router.streamSynthesize("hi", null))
                .expectNextMatches(arr -> java.util.Arrays.equals(arr, audio("a")))
                .expectErrorSatisfies(err -> assertThat(err).hasMessageContaining("mid-stream"))
                .verify();
    }

    @Test
    void backendNamesReturnsImmutableOrderedList() {
        TtsService a = fake("a", audio(""));
        TtsService b = fake("b", audio(""));
        FallbackTtsRouter router = new FallbackTtsRouter(ordered("a", a, "b", b));

        assertThat(router.backendNames()).containsExactly("a", "b");

        // 不可变
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> router.backendNames().add("c"));
    }

    private static TtsService fake(String name, byte[] bytes) {
        return new TtsService() {
            @Override
            public byte[] synthesize(String text, String voice) {
                return bytes;
            }

            @Override
            public Flux<byte[]> streamSynthesize(String text, String voice) {
                return Flux.just(bytes);
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

    private static TtsService fakeStream(String name, List<byte[]> chunks) {
        return new TtsService() {
            @Override
            public byte[] synthesize(String text, String voice) throws Exception {
                throw new UnsupportedOperationException();
            }

            @Override
            public Flux<byte[]> streamSynthesize(String text, String voice) {
                return Flux.fromIterable(chunks);
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

    private static TtsService failing(String name, RuntimeException ex) {
        return new TtsService() {
            @Override
            public byte[] synthesize(String text, String voice) {
                throw ex;
            }

            @Override
            public Flux<byte[]> streamSynthesize(String text, String voice) {
                return Flux.error(ex);
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

    private static TtsService failingStream(String name, RuntimeException ex) {
        return new TtsService() {
            @Override
            public byte[] synthesize(String text, String voice) {
                throw ex;
            }

            @Override
            public Flux<byte[]> streamSynthesize(String text, String voice) {
                return Flux.error(ex);
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

    private static TtsService partialStream(String name, List<byte[]> chunks, RuntimeException errorAfter) {
        return new TtsService() {
            @Override
            public byte[] synthesize(String text, String voice) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Flux<byte[]> streamSynthesize(String text, String voice) {
                return Flux.fromIterable(chunks).concatWith(Flux.error(errorAfter));
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

    private static Map<String, TtsService> ordered(String k1, TtsService v1, String k2, TtsService v2) {
        Map<String, TtsService> map = new LinkedHashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }
}