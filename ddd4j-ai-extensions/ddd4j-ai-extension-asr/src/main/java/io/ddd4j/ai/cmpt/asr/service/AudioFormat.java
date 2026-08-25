package io.ddd4j.ai.cmpt.asr.service;

/**
 * 音频格式描述（转写前的重采样输入）。
 *
 * @param sampleRate    采样率（Hz）
 * @param channels      声道数
 * @param bitsPerSample 位深（8/16/24）
 * @param signed        是否为有符号 PCM
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public record AudioFormat(int sampleRate, int channels, int bitsPerSample, boolean signed) {

    /** 常见 44.1kHz 立体声 16-bit WAV。 */
    public static AudioFormat wav44100Stereo16() {
        return new AudioFormat(44_100, 2, 16, true);
    }
}
