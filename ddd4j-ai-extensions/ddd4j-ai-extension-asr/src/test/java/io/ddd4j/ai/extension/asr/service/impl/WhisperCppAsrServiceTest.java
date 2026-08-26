package io.ddd4j.ai.extension.asr.service.impl;

import io.ddd4j.ai.extension.asr.service.AudioFormat;
import io.github.ggerganov.whispercpp.WhisperCpp;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * {@link WhisperCppAsrService} 单元测试：native 库可用性探测 + 模型缺失错误路径。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class WhisperCppAsrServiceTest {

    @Test
    void transcribe_missingModel_throws() {
        assumeTrue(isNativeAvailable(), "whisper native library not available on this host");
        WhisperCppAsrService service = new WhisperCppAsrService("models/definitely-not-present.bin");
        assertThatThrownBy(() -> service.transcribe(new byte[]{1, 2, 3}, AudioFormat.wav44100Stereo16()))
                .isInstanceOf(Throwable.class);
    }

    @Test
    void transcribe_engineUnavailable_throws() {
        if (isNativeAvailable()) {
            return;
        }
        WhisperCppAsrService service = new WhisperCppAsrService("models/x.bin");
        assertThatThrownBy(() -> service.transcribe(new byte[]{1, 2, 3}, AudioFormat.wav44100Stereo16()))
                .isInstanceOf(Throwable.class);
    }

    private static boolean isNativeAvailable() {
        try {
            new WhisperCpp().close();
            return true;
        } catch (Throwable e) {
            return false;
        }
    }
}
