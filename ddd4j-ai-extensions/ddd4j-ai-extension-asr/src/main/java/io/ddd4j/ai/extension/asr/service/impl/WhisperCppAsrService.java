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

    public WhisperCppAsrService(String modelPath) {
        this.modelPath = modelPath;
    }

    @Override
    public String transcribe(byte[] audio, AudioFormat format) throws Exception {
        float[] samples = WavConverter.toFloatMono16k(audio, format);
        return transcribeSamples(samples);
    }

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
