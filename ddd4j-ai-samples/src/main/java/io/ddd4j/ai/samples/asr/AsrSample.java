package io.ddd4j.ai.samples.asr;

import io.ddd4j.ai.cmpt.asr.service.AsrService;
import io.ddd4j.ai.cmpt.asr.service.AudioFormat;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * asr 离线语音识别组件使用示例：Whisper.cpp 本地转写。
 *
 * <p>前提：引入 {@code ddd4j-ai-extension-asr}，配置 {@code ddd4j.ai.asr.model-path}
 * 指向 ggml 模型文件（如 ggml-base.bin），宿主机支持 whisper native 库。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Component
public class AsrSample {

    private final AsrService asrService;

    public AsrSample(AsrService asrService) {
        this.asrService = asrService;
    }

    /** 转写音频文件（自动重采样为 16kHz 单声道）。 */
    public String transcribeFile(File audio) throws Exception {
        return asrService.transcribe(audio, AudioFormat.wav44100Stereo16());
    }

    /** 转写音频字节（WAV 头自动解析）。 */
    public String transcribeBytes(byte[] audio) throws Exception {
        return asrService.transcribe(audio, AudioFormat.wav44100Stereo16());
    }
}
