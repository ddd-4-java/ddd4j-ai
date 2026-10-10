package io.ddd4j.ai.extension.asr.service.impl;

import io.ddd4j.ai.extension.asr.service.AsrService;
import io.ddd4j.ai.extension.asr.service.AudioFormat;
import io.github.ggerganov.whispercpp.WhisperCpp;
import io.github.ggerganov.whispercpp.params.WhisperFullParams;
import io.github.ggerganov.whispercpp.params.WhisperSamplingStrategy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Whisper.cpp 离线转写实现（JNA 绑定）：模型懒加载，音频自动重采样为 16kHz 单声道。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class WhisperCppAsrService implements AsrService {

    private final String modelPath;
    private volatile WhisperCpp whisper;

    /**
     * 构造转写服务（模型文件此时尚未加载，首次转写时懒加载）。
     *
     * @param modelPath Whisper 模型文件路径（ggml 格式）
     */
    public WhisperCppAsrService(String modelPath) {
        this.modelPath = modelPath;
    }

    /**
     * 转写内存中的音频字节：先解析 WAV 头并重采样为 16kHz 单声道，再交给 Whisper。
     *
     * @param audio  WAV 字节（或裸 PCM）
     * @param format 非 WAV 时的回退音频格式假设
     * @return 转写出的文本
     * @throws Exception 音频转换失败或模型加载/转写出错
     */
    @Override
    public String transcribe(byte[] audio, AudioFormat format) throws Exception {
        float[] samples = WavConverter.toFloatMono16k(audio, format);
        return transcribeSamples(samples);
    }

    /**
     * 转写音频文件：读取全部字节后委托给字节版转写。
     *
     * @param audio  音频文件
     * @param format 非 WAV 时的回退音频格式假设
     * @return 转写出的文本
     * @throws Exception 读取文件失败或模型加载/转写出错
     */
    @Override
    public String transcribe(File audio, AudioFormat format) throws Exception {
        return transcribe(Files.readAllBytes(audio.toPath()), format);
    }

    private String transcribeSamples(float[] samples) throws IOException {
        WhisperCpp engine = ensureLoaded();
        WhisperFullParams params = engine.getFullDefaultParams(WhisperSamplingStrategy.WHISPER_SAMPLING_GREEDY);
        return engine.fullTranscribe(params, samples);
    }

    private WhisperCpp ensureLoaded() throws IOException {
        if (whisper == null) {
            synchronized (this) {
                if (whisper == null) {
                    WhisperCpp instance = new WhisperCpp();
                    instance.initContext(modelPath);
                    whisper = instance;
                }
            }
        }
        return whisper;
    }
}
