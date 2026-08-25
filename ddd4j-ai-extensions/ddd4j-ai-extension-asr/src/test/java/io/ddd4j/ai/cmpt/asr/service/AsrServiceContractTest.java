package io.ddd4j.ai.cmpt.asr.service;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link AsrService} 端口契约测试：转写语义 / 异常传播。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class AsrServiceContractTest {

    static class FakeAsrService implements AsrService {

        private final RuntimeException failure;

        FakeAsrService() {
            this(null);
        }

        FakeAsrService(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public String transcribe(byte[] audio, AudioFormat format) {
            if (failure != null) {
                throw failure;
            }
            return "recognized";
        }

        @Override
        public String transcribe(File audio, AudioFormat format) throws Exception {
            if (failure != null) {
                throw failure;
            }
            return "recognized";
        }
    }

    @Test
    void transcribe_returnsText() throws Exception {
        assertThat(new FakeAsrService().transcribe(new byte[]{1, 2, 3}, AudioFormat.wav44100Stereo16()))
                .isEqualTo("recognized");
    }

    @Test
    void transcribe_propagatesEngineFailure() {
        RuntimeException cause = new IllegalStateException("engine down");
        assertThatThrownBy(() -> new FakeAsrService(cause)
                .transcribe(new byte[]{1}, AudioFormat.wav44100Stereo16()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("engine down");
    }

    @Test
    void audioFormat_commonWavPreset() {
        AudioFormat format = AudioFormat.wav44100Stereo16();
        assertThat(format.sampleRate()).isEqualTo(44_100);
        assertThat(format.channels()).isEqualTo(2);
        assertThat(format.bitsPerSample()).isEqualTo(16);
        assertThat(format.signed()).isTrue();
    }
}
