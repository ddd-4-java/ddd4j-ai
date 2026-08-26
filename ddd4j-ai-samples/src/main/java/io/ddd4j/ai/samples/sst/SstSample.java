package io.ddd4j.ai.samples.sst;

import com.microsoft.cognitiveservices.speech.SpeechConfig;
import io.ddd4j.ai.extension.sst.service.SpeechService;
import io.ddd4j.ai.extension.sst.service.SpeechServiceText2VoiceCallback;
import io.ddd4j.ai.extension.sst.service.SpeechServiceVoice2TextCallback;
import io.ddd4j.ai.extension.sst.service.impl.FFmpegService;
import io.ddd4j.ai.extension.sst.vo.TTSResultVO;
import org.springframework.stereotype.Component;

/**
 * sst 语音组件使用示例：演示业务服务中注入 {@link SpeechService} 与 {@link FFmpegService}
 * 完成 TTS（文本转语音）与 STT（语音转文本，含 FFmpeg 前置格式归一化）的典型用法。
 *
 * <p>前提：
 * <ul>
 *   <li>业务服务继承 {@code ddd4j-ai-parent}（或 import {@code ddd4j-ai-bom}）后引入
 *       {@code ddd4j-ai-extension-sst}；</li>
 *   <li>{@code application.yml} 配置 {@code azure.speech.*}（key/region/voice-name/recognition-language）；</li>
 *   <li>宿主机安装 ffmpeg 并在 PATH 中（STT 前置转换与 MP3 输出依赖它）。</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Component
public class SstSample {

    private final SpeechService<SpeechConfig> speechService;

    private final FFmpegService ffmpegService;

    public SstSample(SpeechService<SpeechConfig> speechService, FFmpegService ffmpegService) {
        this.speechService = speechService;
        this.ffmpegService = ffmpegService;
    }

    /**
     * 文本转语音（同步）：返回 16kHz 32kbps 单声道 MP3 字节流，可直接落盘或写 HTTP 响应。
     */
    public byte[] speak(String text) throws Exception {
        TTSResultVO result = speechService.tts(null, text);
        return result.getAudio();
    }

    /**
     * 文本转语音（回调）：适合流式消费合成音频的场景。
     */
    public void speakAsync(String text, SpeechServiceText2VoiceCallback callback) throws Exception {
        speechService.text2Voice(text, callback);
    }

    /**
     * 语音转文本：任意格式音频先经 FFmpeg 归一化为 16kHz 单声道 WAV（Azure STT 推荐格式）再识别。
     */
    public void listen(byte[] arbitraryAudio, SpeechServiceVoice2TextCallback callback) throws Exception {
        byte[] wav = ffmpegService.convertAudioToWavFromByteAry(arbitraryAudio);
        speechService.voice2TextFromWavByteArray(null, wav, callback);
    }

    /**
     * 语音转文本：直接识别 MP3 字节流（Azure SDK 原生支持压缩格式）。
     */
    public void listenMp3(byte[] mp3Audio, SpeechServiceVoice2TextCallback callback) throws Exception {
        speechService.voice2TextFromMp3ByteArray(null, mp3Audio, callback);
    }
}
