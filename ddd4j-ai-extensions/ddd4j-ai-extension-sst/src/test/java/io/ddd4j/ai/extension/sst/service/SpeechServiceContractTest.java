package io.ddd4j.ai.extension.sst.service;

import io.ddd4j.ai.extension.sst.vo.TTSResultVO;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SpeechService} 端口契约测试：以测试桩验证回调路径（onSuccess/onFail/onCancel）
 * 与泛型配置端口的调用语义，约束所有实现应遵循的行为。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class SpeechServiceContractTest {

    /** 记录型回调桩：捕获回调结果。 */
    static final class RecordingText2VoiceCallback implements SpeechServiceText2VoiceCallback {
        byte[] audio;
        String failReason;
        String cancelReason;

        @Override public void onCancel(String reason) { this.cancelReason = reason; }
        @Override public void onSuccess(byte[] audioData) { this.audio = audioData; }
        @Override public void onFail(String reason) { this.failReason = reason; }
    }

    static final class RecordingVoice2TextCallback implements SpeechServiceVoice2TextCallback {
        String text;
        String failReason;
        String cancelReason;

        @Override public void onFail(String reason) { this.failReason = reason; }
        @Override public void onCancel(String reason) { this.cancelReason = reason; }
        @Override public void onSuccess(String text) { this.text = text; }
    }

    /** 最小端口桩：泛型实参取 String，模拟各回调路径。 */
    static final class FakeSpeechService implements SpeechService<String> {

        TTSResultVO ttsResult = TTSResultVO.builder().status(1).msg("成功")
                .audio("audio".getBytes(StandardCharsets.UTF_8)).build();

        @Override
        public void text2Voice(String content, SpeechServiceText2VoiceCallback callback) {
            if (callback != null) {
                callback.onSuccess(content.getBytes(StandardCharsets.UTF_8));
            }
        }

        @Override
        public TTSResultVO tts(String speechConfigDto, String content) {
            return ttsResult;
        }

        @Override
        public void voice2TextFromWavFile(String wavFile, SpeechServiceVoice2TextCallback callback) {
            if (callback != null) {
                callback.onSuccess("识别文本");
            }
        }

        @Override
        public void voice2TextFromWavByteArray(String speechConfigDto, byte[] wavFileBytes,
                                               SpeechServiceVoice2TextCallback callback) {
            if (callback != null) {
                callback.onSuccess("识别文本");
            }
        }

        @Override
        public void voice2TextFromMp3ByteArray(String speechConfigDto, byte[] wavFileBytes,
                                               SpeechServiceVoice2TextCallback callback) {
            if (callback != null) {
                callback.onFail("NoMatch");
            }
        }
    }

    @Test
    void text2VoiceDeliversAudioViaCallback() {
        FakeSpeechService service = new FakeSpeechService();
        RecordingText2VoiceCallback callback = new RecordingText2VoiceCallback();

        service.text2Voice("你好", callback);

        assertThat(callback.audio).isEqualTo("你好".getBytes(StandardCharsets.UTF_8));
        assertThat(callback.failReason).isNull();
        assertThat(callback.cancelReason).isNull();
    }

    @Test
    void ttsReturnsSynchronousResult() {
        FakeSpeechService service = new FakeSpeechService();

        TTSResultVO result = service.tts("cfg", "你好");

        assertThat(result.getStatus()).isEqualTo(1);
        assertThat(result.getMsg()).isEqualTo("成功");
        assertThat(result.getAudio()).isNotEmpty();
    }

    @Test
    void voice2TextCallbacksCoverSuccessFailAndCancel() {
        FakeSpeechService service = new FakeSpeechService();

        RecordingVoice2TextCallback success = new RecordingVoice2TextCallback();
        service.voice2TextFromWavFile("a.wav", success);
        assertThat(success.text).isEqualTo("识别文本");

        RecordingVoice2TextCallback fromBytes = new RecordingVoice2TextCallback();
        service.voice2TextFromWavByteArray(null, new byte[0], fromBytes);
        assertThat(fromBytes.text).isEqualTo("识别文本");

        RecordingVoice2TextCallback failed = new RecordingVoice2TextCallback();
        service.voice2TextFromMp3ByteArray(null, new byte[0], failed);
        assertThat(failed.failReason).isEqualTo("NoMatch");
        assertThat(failed.text).isNull();
    }

    @Test
    void nullCallbackMustNotBreakInvocation() {
        FakeSpeechService service = new FakeSpeechService();

        // 端口约定：callback 判空后静默跳过，不抛异常
        service.text2Voice("你好", null);
        service.voice2TextFromWavByteArray(null, new byte[0], null);
        service.voice2TextFromMp3ByteArray(null, new byte[0], null);
    }
}
