package io.ddd4j.ai.samples.asr;

import io.ddd4j.ai.extension.asr.service.AsrService;
import io.ddd4j.ai.extension.asr.service.AudioFormat;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * asr 离线语音识别组件使用示例：Whisper.cpp 本地转写。
 *
 * <p>前提：引入 {@code ddd4j-ai-extension-asr}，配置 {@code ddd4j.ai.asr.model-path}
 * 指向 ggml 模型文件（如 ggml-base.bin），宿主机支持 whisper native 库。
 *
 * @author ddd4j
 */
@Component
public class AsrSample {

    private final AsrService asrService;

    /**
     * 构造 AsrSample。
     *
     * @param asrService 语音识别服务
     */
    public AsrSample(AsrService asrService) {
        this.asrService = asrService;
    }

    /**
     * 转写音频文件（自动重采样为 16kHz 单声道）。
     *
     * @param audio 音频数据
     * @return 转写得到的文本
     * @throws Exception 执行过程中出现异常时抛出
     */
    public String transcribeFile(File audio) throws Exception {
        return asrService.transcribe(audio, AudioFormat.wav44100Stereo16());
    }

    /**
     * 转写音频字节（WAV 头自动解析）。
     *
     * @param audio 音频数据
     * @return 转写得到的文本
     * @throws Exception 执行过程中出现异常时抛出
     */
    public String transcribeBytes(byte[] audio) throws Exception {
        return asrService.transcribe(audio, AudioFormat.wav44100Stereo16());
    }
}
