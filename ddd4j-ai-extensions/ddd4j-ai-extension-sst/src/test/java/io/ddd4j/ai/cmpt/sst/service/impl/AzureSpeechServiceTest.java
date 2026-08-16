package io.ddd4j.ai.cmpt.sst.service.impl;

import com.microsoft.cognitiveservices.speech.CancellationDetails;
import com.microsoft.cognitiveservices.speech.CancellationReason;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisCancellationDetails;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.audio.AudioInputStream;
import com.microsoft.cognitiveservices.speech.audio.PushAudioInputStream;
import io.ddd4j.ai.cmpt.sst.properties.AzureSpeechProperties;
import io.ddd4j.ai.cmpt.sst.service.SpeechServiceText2VoiceCallback;
import io.ddd4j.ai.cmpt.sst.service.SpeechServiceVoice2TextCallback;
import io.ddd4j.ai.cmpt.sst.vo.TTSResultVO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AzureSpeechService} 单元测试：mock Azure Speech SDK 隔离网络与 native 依赖，
 * 验证 afterPropertiesSet / tts / text2Voice / voice2TextFromWavByteArray 的成功与失败分支。
 * 环境无法加载 Azure SDK native 库时整类跳过。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class AzureSpeechServiceTest {

    /** 记录型 TTS 回调。 */
    static class RecText2Voice implements SpeechServiceText2VoiceCallback {
        byte[] audio;
        String fail;
        String cancel;

        @Override public void onCancel(String reason) { this.cancel = reason; }
        @Override public void onSuccess(byte[] audioData) { this.audio = audioData; }
        @Override public void onFail(String reason) { this.fail = reason; }
    }

    /** 记录型 STT 回调。 */
    static class RecVoice2Text implements SpeechServiceVoice2TextCallback {
        String text;
        String fail;
        String cancel;

        @Override public void onFail(String reason) { this.fail = reason; }
        @Override public void onCancel(String reason) { this.cancel = reason; }
        @Override public void onSuccess(String text) { this.text = text; }
    }

    @BeforeAll
    static void requireAzureSdkNative() {
        boolean loadable;
        try {
            // 触发 SDK 静态初始化（含 native 库加载）
            Class.forName("com.microsoft.cognitiveservices.speech.SpeechConfig", true,
                    AzureSpeechServiceTest.class.getClassLoader());
            loadable = true;
        } catch (Throwable t) {
            loadable = false;
        }
        assumeTrue(loadable, "Azure Speech SDK native 库不可用，跳过 AzureSpeechService 测试");
    }

    private static AzureSpeechProperties properties() {
        AzureSpeechProperties properties = new AzureSpeechProperties();
        properties.setKey("test-key");
        properties.setRegion("eastasia");
        properties.setVoiceName("zh-CN-XiaoxiaoNeural");
        properties.setRecognitionLanguage("zh-CN");
        return properties;
    }

    /** 用原生反射注入 properties 字段，避免对 spring-test 的依赖。 */
    private static AzureSpeechService newService() throws Exception {
        AzureSpeechService service = new AzureSpeechService();
        Field field = AzureSpeechService.class.getDeclaredField("azureSpeechProperties");
        field.setAccessible(true);
        field.set(service, properties());
        return service;
    }

    // ---------- afterPropertiesSet ----------

    @Test
    void afterPropertiesSetBuildsConfigFromProperties() throws Exception {
        SpeechConfig configMock = mock(SpeechConfig.class);

        try (MockedStatic<SpeechConfig> speechConfigStatic = mockStatic(SpeechConfig.class)) {
            speechConfigStatic.when(() -> SpeechConfig.fromSubscription("test-key", "eastasia"))
                    .thenReturn(configMock);

            newService().afterPropertiesSet();

            speechConfigStatic.verify(() -> SpeechConfig.fromSubscription("test-key", "eastasia"));
            verify(configMock).setSpeechSynthesisVoiceName("zh-CN-XiaoxiaoNeural");
            verify(configMock).setSpeechRecognitionLanguage("zh-CN");
        }
    }

    // ---------- tts：同步合成 ----------

    @Test
    void ttsSuccessReturnsAudio() throws Exception {
        byte[] audio = new byte[]{1, 2, 3};
        SpeechConfig configMock = mock(SpeechConfig.class);
        SpeechSynthesisResult resultMock = mock(SpeechSynthesisResult.class);
        when(resultMock.getReason()).thenReturn(ResultReason.SynthesizingAudioCompleted);
        when(resultMock.getAudioData()).thenReturn(audio);

        try (MockedStatic<SpeechConfig> speechConfigStatic = mockStatic(SpeechConfig.class);
             MockedConstruction<SpeechSynthesizer> ignored = mockConstruction(SpeechSynthesizer.class,
                     (synth, ctx) -> when(synth.SpeakTextAsync("你好"))
                             .thenReturn(CompletableFuture.completedFuture(resultMock)))) {

            speechConfigStatic.when(() -> SpeechConfig.fromSubscription(anyString(), anyString()))
                    .thenReturn(configMock);

            AzureSpeechService service = newService();
            service.afterPropertiesSet();

            TTSResultVO result = service.tts(null, "你好");

            assertThat(result.getStatus()).isEqualTo(1);
            assertThat(result.getMsg()).isEqualTo("成功");
            assertThat(result.getAudio()).isEqualTo(audio);
        }
    }

    @Test
    void ttsCanceledReturnsFailureVo() throws Exception {
        SpeechConfig configMock = mock(SpeechConfig.class);
        SpeechSynthesisResult resultMock = mock(SpeechSynthesisResult.class);
        when(resultMock.getReason()).thenReturn(ResultReason.Canceled);

        SpeechSynthesisCancellationDetails detailsMock = mock(SpeechSynthesisCancellationDetails.class);
        when(detailsMock.getReason()).thenReturn(CancellationReason.Error);
        when(detailsMock.getErrorDetails()).thenReturn("subscription invalid");

        try (MockedStatic<SpeechConfig> speechConfigStatic = mockStatic(SpeechConfig.class);
             MockedStatic<SpeechSynthesisCancellationDetails> detailsStatic = mockStatic(SpeechSynthesisCancellationDetails.class);
             MockedConstruction<SpeechSynthesizer> ignored = mockConstruction(SpeechSynthesizer.class,
                     (synth, ctx) -> when(synth.SpeakTextAsync(anyString()))
                             .thenReturn(CompletableFuture.completedFuture(resultMock)))) {

            speechConfigStatic.when(() -> SpeechConfig.fromSubscription(anyString(), anyString()))
                    .thenReturn(configMock);
            detailsStatic.when(() -> SpeechSynthesisCancellationDetails.fromResult(resultMock))
                    .thenReturn(detailsMock);

            AzureSpeechService service = newService();
            service.afterPropertiesSet();

            TTSResultVO result = service.tts(null, "你好");

            // 现状语义：失败时 status 同为 1、audio 为空（存量行为，如实断言；建议后续迭代修正为独立失败码）
            assertThat(result.getStatus()).isEqualTo(1);
            assertThat(result.getMsg()).isEqualTo("失败");
            assertThat(result.getAudio()).isNull();
        }
    }

    // ---------- text2Voice：回调合成 ----------

    @Test
    void text2VoiceSuccessInvokesOnSuccess() throws Exception {
        byte[] audio = new byte[]{9, 8, 7};
        SpeechConfig configMock = mock(SpeechConfig.class);
        SpeechSynthesisResult resultMock = mock(SpeechSynthesisResult.class);
        when(resultMock.getReason()).thenReturn(ResultReason.SynthesizingAudioCompleted);
        when(resultMock.getAudioData()).thenReturn(audio);

        try (MockedStatic<SpeechConfig> speechConfigStatic = mockStatic(SpeechConfig.class);
             MockedConstruction<SpeechSynthesizer> ignored = mockConstruction(SpeechSynthesizer.class,
                     (synth, ctx) -> when(synth.SpeakTextAsync("你好"))
                             .thenReturn(CompletableFuture.completedFuture(resultMock)))) {

            speechConfigStatic.when(() -> SpeechConfig.fromSubscription(anyString(), anyString()))
                    .thenReturn(configMock);

            AzureSpeechService service = newService();
            service.afterPropertiesSet();

            RecText2Voice callback = new RecText2Voice();
            service.text2Voice("你好", callback);

            assertThat(callback.audio).isEqualTo(audio);
            assertThat(callback.fail).isNull();
        }
    }

    @Test
    void text2VoiceCanceledInvokesOnFail() throws Exception {
        SpeechConfig configMock = mock(SpeechConfig.class);
        SpeechSynthesisResult resultMock = mock(SpeechSynthesisResult.class);
        when(resultMock.getReason()).thenReturn(ResultReason.Canceled);

        SpeechSynthesisCancellationDetails detailsMock = mock(SpeechSynthesisCancellationDetails.class);
        when(detailsMock.getReason()).thenReturn(CancellationReason.Error);
        when(detailsMock.getErrorDetails()).thenReturn("quota exceeded");

        try (MockedStatic<SpeechConfig> speechConfigStatic = mockStatic(SpeechConfig.class);
             MockedStatic<SpeechSynthesisCancellationDetails> detailsStatic = mockStatic(SpeechSynthesisCancellationDetails.class);
             MockedConstruction<SpeechSynthesizer> ignored = mockConstruction(SpeechSynthesizer.class,
                     (synth, ctx) -> when(synth.SpeakTextAsync(anyString()))
                             .thenReturn(CompletableFuture.completedFuture(resultMock)))) {

            speechConfigStatic.when(() -> SpeechConfig.fromSubscription(anyString(), anyString()))
                    .thenReturn(configMock);
            detailsStatic.when(() -> SpeechSynthesisCancellationDetails.fromResult(resultMock))
                    .thenReturn(detailsMock);

            AzureSpeechService service = newService();
            service.afterPropertiesSet();

            RecText2Voice callback = new RecText2Voice();
            service.text2Voice("你好", callback);

            assertThat(callback.fail).isEqualTo("quota exceeded");
            assertThat(callback.audio).isNull();
        }
    }

    // ---------- voice2TextFromWavByteArray：识别分支 ----------

    /** 组装 STT 路径的全部 mock 资源（调用方在 try-with-resources 中逐个关闭）。 */
    record SttMocks(MockedStatic<SpeechConfig> speechConfigStatic,
                    MockedStatic<AudioInputStream> audioStreamStatic,
                    MockedStatic<AudioConfig> audioConfigStatic,
                    MockedStatic<CancellationDetails> detailsStatic,
                    MockedConstruction<SpeechRecognizer> recognizerCtor) implements AutoCloseable {

        static SttMocks setup(ResultReason reason, String text, String errorDetails) {
            SpeechConfig configMock = mock(SpeechConfig.class);
            PushAudioInputStream pushMock = mock(PushAudioInputStream.class);
            AudioConfig audioConfigMock = mock(AudioConfig.class);
            SpeechRecognitionResult resultMock = mock(SpeechRecognitionResult.class);
            when(resultMock.getReason()).thenReturn(reason);
            when(resultMock.getText()).thenReturn(text);

            MockedStatic<SpeechConfig> speechConfigStatic = mockStatic(SpeechConfig.class);
            speechConfigStatic.when(() -> SpeechConfig.fromSubscription(anyString(), anyString()))
                    .thenReturn(configMock);

            MockedStatic<AudioInputStream> audioStreamStatic = mockStatic(AudioInputStream.class);
            audioStreamStatic.when(AudioInputStream::createPushStream).thenReturn(pushMock);

            MockedStatic<AudioConfig> audioConfigStatic = mockStatic(AudioConfig.class);
            audioConfigStatic.when(() -> AudioConfig.fromStreamInput(pushMock)).thenReturn(audioConfigMock);

            MockedStatic<CancellationDetails> detailsStatic = mockStatic(CancellationDetails.class);
            if (errorDetails != null) {
                CancellationDetails detailsMock = mock(CancellationDetails.class);
                when(detailsMock.getReason()).thenReturn(CancellationReason.Error);
                when(detailsMock.getErrorDetails()).thenReturn(errorDetails);
                detailsStatic.when(() -> CancellationDetails.fromResult(resultMock)).thenReturn(detailsMock);
            }

            MockedConstruction<SpeechRecognizer> recognizerCtor = mockConstruction(SpeechRecognizer.class,
                    (recognizer, ctx) -> when(recognizer.recognizeOnceAsync())
                            .thenReturn(CompletableFuture.completedFuture(resultMock)));

            return new SttMocks(speechConfigStatic, audioStreamStatic, audioConfigStatic,
                    detailsStatic, recognizerCtor);
        }

        @Override
        public void close() {
            recognizerCtor.close();
            detailsStatic.close();
            audioConfigStatic.close();
            audioStreamStatic.close();
            speechConfigStatic.close();
        }
    }

    @Test
    void voice2TextRecognizedInvokesOnSuccess() throws Exception {
        try (SttMocks mocks = SttMocks.setup(ResultReason.RecognizedSpeech, "你好世界", null)) {
            AzureSpeechService service = newService();
            service.afterPropertiesSet();

            RecVoice2Text callback = new RecVoice2Text();
            service.voice2TextFromWavByteArray(null, new byte[]{0, 1}, callback);

            assertThat(callback.text).isEqualTo("你好世界");
            assertThat(callback.fail).isNull();
            assertThat(callback.cancel).isNull();
        }
    }

    @Test
    void voice2TextNoMatchInvokesOnFail() throws Exception {
        try (SttMocks mocks = SttMocks.setup(ResultReason.NoMatch, null, null)) {
            AzureSpeechService service = newService();
            service.afterPropertiesSet();

            RecVoice2Text callback = new RecVoice2Text();
            service.voice2TextFromWavByteArray(null, new byte[]{0, 1}, callback);

            assertThat(callback.fail).isEqualTo("NoMatch");
            assertThat(callback.text).isNull();
        }
    }

    @Test
    void voice2TextCanceledInvokesOnCancel() throws Exception {
        try (SttMocks mocks = SttMocks.setup(ResultReason.Canceled, null, "connection lost")) {
            AzureSpeechService service = newService();
            service.afterPropertiesSet();

            RecVoice2Text callback = new RecVoice2Text();
            service.voice2TextFromWavByteArray(null, new byte[]{0, 1}, callback);

            assertThat(callback.cancel).isEqualTo("connection lost");
        }
    }
}
