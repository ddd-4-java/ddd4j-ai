package io.ddd4j.ai.extension.tts.service;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link TtsService} 端口契约测试：合成语义 / 流式 / 异常传播。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class TtsServiceContractTest {

    static class FakeTtsService implements TtsService {

        private final RuntimeException failure;

        FakeTtsService() {
            this(null);
        }

        FakeTtsService(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public byte[] synthesize(String text, String voice) {
            if (failure != null) {
                throw failure;
            }
            return text.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public Flux<byte[]> streamSynthesize(String text, String voice) {
            if (failure != null) {
                return Flux.error(failure);
            }
            return Flux.just(synthesize(text, voice));
        }
    }

    @Test
    void synthesize_returnsAudioBytes() throws Exception {
        byte[] audio = new FakeTtsService().synthesize("hello", "zh-CN-XiaoxiaoNeural");
        assertThat(audio).isNotEmpty();
        assertThat(new String(audio, StandardCharsets.UTF_8)).isEqualTo("hello");
    }

    @Test
    void synthesize_propagatesEngineFailure() {
        RuntimeException cause = new IllegalStateException("network down");
        assertThatThrownBy(() -> new FakeTtsService(cause).synthesize("hi", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("network down");
    }

    @Test
    void streamSynthesize_emitsAudioChunks() {
        List<byte[]> chunks = new FakeTtsService().streamSynthesize("hi", null).collectList().block();
        assertThat(chunks).hasSize(1);
        assertThat(new String(chunks.get(0), StandardCharsets.UTF_8)).isEqualTo("hi");
    }

    @Test
    void streamSynthesize_propagatesEngineFailure() {
        RuntimeException cause = new IllegalStateException("network down");
        List<byte[]> chunks = new FakeTtsService(cause).streamSynthesize("hi", null)
                .onErrorResume(e -> Flux.just(e.getMessage().getBytes(StandardCharsets.UTF_8)))
                .collectList().block();
        assertThat(new String(chunks.get(0), StandardCharsets.UTF_8)).isEqualTo("network down");
    }
}
