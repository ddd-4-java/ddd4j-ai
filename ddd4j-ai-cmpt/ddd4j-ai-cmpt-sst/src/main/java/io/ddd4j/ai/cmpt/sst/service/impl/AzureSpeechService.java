package io.ddd4j.ai.cmpt.sst.service.impl;

import cn.hutool.core.io.FileUtil;
import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.*;
import io.ddd4j.ai.cmpt.sst.properties.AzureSpeechProperties;
import io.ddd4j.ai.cmpt.sst.service.SpeechService;
import io.ddd4j.ai.cmpt.sst.service.SpeechServiceText2VoiceCallback;
import io.ddd4j.ai.cmpt.sst.service.SpeechServiceVoice2TextCallback;
import io.ddd4j.ai.cmpt.sst.vo.TTSResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.Future;

/**
 * @Title: AzureSpeechService
 * @Package com.easy.learn.tts
 * @Author: czq
 * @Date: 2024年06月27日 9:47
 * @Desc:
 */

@Slf4j
@Component
public class AzureSpeechService implements SpeechService<SpeechConfig>, InitializingBean {

//    @Value("${tts.azure.speech.key:bc0f7f96d1854214832adca4e143b3d5}")
//    private String speechKey;
//
//    @Value("${tts.azure.speech.region:eastasia}")
//    private String speechRegion;
//
//    @Value("${tts.azure.speech.voiceName:zh-CN-XiaoxiaoNeural}")
//    private String speechSynthesisVoiceName;
//
//    @Value("${tts.azure.speech.recognitionLanguage:zh-CN}")
//    private String speechRecognitionLanguage;

    private SpeechConfig speechConfig;


    @Autowired
    private AzureSpeechProperties azureSpeechProperties;


    @Override
    public void afterPropertiesSet() throws Exception {

        String speechKey = azureSpeechProperties.getKey();
        String speechRegion = azureSpeechProperties.getRegion();
        String speechSynthesisVoiceName = azureSpeechProperties.getVoiceName();
        String speechRecognitionLanguage = azureSpeechProperties.getRecognitionLanguage();


        speechConfig = SpeechConfig.fromSubscription(speechKey, speechRegion);
        speechConfig.setSpeechSynthesisVoiceName(speechSynthesisVoiceName);
        speechConfig.setSpeechRecognitionLanguage(speechRecognitionLanguage);

    }

    public SpeechConfig getDefaultSpeechSTTConfig() {
        String speechKey = azureSpeechProperties.getKey();
        String speechRegion = azureSpeechProperties.getRegion();
        String speechSynthesisVoiceName = azureSpeechProperties.getVoiceName();
        String speechRecognitionLanguage = azureSpeechProperties.getRecognitionLanguage();
        SpeechConfig speechConfig = SpeechConfig.fromSubscription(speechKey, speechRegion);
        speechConfig.setSpeechRecognitionLanguage(speechRecognitionLanguage);
        speechConfig.setProperty(PropertyId.Speech_SegmentationSilenceTimeoutMs, "1000");
        return speechConfig;
    }


    @Override
    public void text2Voice(String content, SpeechServiceText2VoiceCallback callback) throws Exception {

        SpeechSynthesizer speechSynthesizer = new SpeechSynthesizer(speechConfig);
        SpeechSynthesisResult speechSynthesisResult =
                speechSynthesizer.SpeakTextAsync(content).get();

        ResultReason reason = speechSynthesisResult.getReason();
        if (reason == ResultReason.SynthesizingAudioCompleted) {
            log.info("Speech synthesized to speaker for text [" + content + "]");
            byte[] audioData = speechSynthesisResult.getAudioData();
            speechSynthesizer.close();
            if (Objects.nonNull(callback)) {
                callback.onSuccess(audioData);
            }
            return;
        }
        if (reason == ResultReason.Canceled) {
            SpeechSynthesisCancellationDetails cancellation = SpeechSynthesisCancellationDetails.fromResult(speechSynthesisResult);
            log.error("CANCELED: Reason=" + cancellation.getReason());
            CancellationReason cancelReason = cancellation.getReason();
            if (cancelReason == CancellationReason.Error) {
                log.error("CANCELED: ErrorCode=" + cancellation.getErrorCode());
                log.error("CANCELED: ErrorDetails=" + cancellation.getErrorDetails());
                log.error("CANCELED: Did you set the speech resource key and region values?");
            }
            speechSynthesizer.close();
            if (Objects.nonNull(callback)) {
                callback.onFail(cancellation.getErrorDetails());
            }
            return;
        }
        speechSynthesizer.close();
    }

    @Override
    public TTSResultVO tts(SpeechConfig speechConfigDto, String content) throws Exception {
        SpeechConfig config = speechConfig;
        if (Objects.nonNull(speechConfigDto)) {
            config = speechConfigDto;
        }
        config.setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Audio16Khz32KBitRateMonoMp3);
        SpeechSynthesizer speechSynthesizer = new SpeechSynthesizer(config);
        SpeechSynthesisResult speechSynthesisResult =
                speechSynthesizer.SpeakTextAsync(content).get();

        ResultReason reason = speechSynthesisResult.getReason();
        try {
            if (reason == ResultReason.SynthesizingAudioCompleted) {
                log.info("Speech synthesized to speaker for text [" + content + "]");
                byte[] audioData = speechSynthesisResult.getAudioData();
                speechSynthesizer.close();
                return TTSResultVO.builder().msg("成功")
                        .status(1).audio(audioData).build();
            }
            if (reason == ResultReason.Canceled) {
                SpeechSynthesisCancellationDetails cancellation = SpeechSynthesisCancellationDetails.fromResult(speechSynthesisResult);
                log.error("CANCELED: Reason=" + cancellation.getReason());
                CancellationReason cancelReason = cancellation.getReason();
                if (cancelReason == CancellationReason.Error) {
                    log.error("CANCELED: ErrorCode=" + cancellation.getErrorCode());
                    log.error("CANCELED: ErrorDetails=" + cancellation.getErrorDetails());
                    log.error("CANCELED: Did you set the speech resource key and region values?");
                }
                speechSynthesizer.close();
            }
        } finally {
            speechSynthesizer.close();
        }
        return TTSResultVO.builder().msg("失败")
                .status(1).audio(null).build();
    }

    @Override
    public void voice2TextFromWavFile(String wavFile, SpeechServiceVoice2TextCallback callback) throws Exception {
        PushAudioInputStream pushStream = AudioInputStream.createPushStream();
        AudioConfig audioConfig = AudioConfig.fromStreamInput(pushStream);
        SpeechRecognizer speechRecognizer = new SpeechRecognizer(speechConfig, audioConfig);

        byte[] bytes = FileUtil.readBytes(wavFile);
        pushStream.write(bytes);
        //必须调用close 才会进行发送，不然会超时
        pushStream.close();
        doVoice2Text(speechRecognizer, callback);

    }

    @Override
    public void voice2TextFromWavByteArray(SpeechConfig speechConfigDto,byte[] wavFileBytes, SpeechServiceVoice2TextCallback callback) throws Exception {
        SpeechConfig config = speechConfig;
        if (Objects.nonNull(speechConfigDto)) {
            config = speechConfigDto;
        }


        PushAudioInputStream pushStream = AudioInputStream.createPushStream();
        AudioConfig audioConfig = AudioConfig.fromStreamInput(pushStream);
        SpeechRecognizer speechRecognizer = new SpeechRecognizer(config, audioConfig);

        pushStream.write(wavFileBytes);
        //必须调用close 才会进行发送，不然会超时
        pushStream.close();

        doVoice2Text(speechRecognizer, callback);
    }

    @Override
    public void voice2TextFromMp3ByteArray(SpeechConfig speechConfigDto,byte[] wavFileBytes, SpeechServiceVoice2TextCallback callback) throws Exception {
        SpeechConfig config = speechConfig;
        if (Objects.nonNull(speechConfigDto)) {
            config = speechConfigDto;
        }
        PushAudioInputStream pushStream = AudioInputStream.createPushStream(AudioStreamFormat.getCompressedFormat(AudioStreamContainerFormat.MP3));


        AudioConfig audioConfig = AudioConfig.fromStreamInput(pushStream);

        SpeechRecognizer speechRecognizer = new SpeechRecognizer(config, audioConfig);

        pushStream.write(wavFileBytes);
        //必须调用close 才会进行发送，不然会超时
        pushStream.close();

        doVoice2Text(speechRecognizer, callback);
    }

    private void doVoice2Text(SpeechRecognizer speechRecognizer, SpeechServiceVoice2TextCallback callback) throws Exception {
        Future<SpeechRecognitionResult> task = speechRecognizer.recognizeOnceAsync();
        SpeechRecognitionResult speechRecognitionResult = task.get();

        ResultReason reason = speechRecognitionResult.getReason();

        if (reason == ResultReason.RecognizedSpeech) {
            log.info("RECOGNIZED: Text=" + speechRecognitionResult.getText());
            speechRecognizer.close();
            if (Objects.nonNull(callback)) {
                callback.onSuccess(speechRecognitionResult.getText());
            }
            return;
        }
        if (reason == ResultReason.NoMatch) {
            log.error("不匹配: 语音不能被识别！");
            speechRecognizer.close();
            if (Objects.nonNull(callback)) {
                callback.onFail(reason.name());
            }
            return;
        }
        if (reason == ResultReason.Canceled) {
            CancellationDetails cancellation = CancellationDetails.fromResult(speechRecognitionResult);
            System.out.println("CANCELED: Reason=" + cancellation.getReason());
            if (cancellation.getReason() == CancellationReason.Error) {
                log.error("CANCELED: ErrorCode=" + cancellation.getErrorCode());
                log.error("CANCELED: ErrorDetails=" + cancellation.getErrorDetails());
            }
            speechRecognizer.close();
            if (Objects.nonNull(callback)) {
                callback.onCancel(cancellation.getErrorDetails());
            }
            return;
        }
        speechRecognizer.close();
    }


}
