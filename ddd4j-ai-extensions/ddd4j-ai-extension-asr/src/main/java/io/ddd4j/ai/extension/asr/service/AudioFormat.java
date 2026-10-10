package io.ddd4j.ai.extension.asr.service;

import java.beans.ConstructorProperties;

import java.util.Objects;

/**
 * 音频格式描述（转写前的重采样输入）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */

public final class AudioFormat {

    private static final long serialVersionUID = 0L;

    private final int sampleRate;

    private final int channels;

    private final int bitsPerSample;

    private final boolean signed;

    /**
     * 常见 44.1kHz 立体声 16-bit WAV。
     */
    public static AudioFormat wav44100Stereo16() {
        return new AudioFormat(44_100, 2, 16, true);
    }

    /**
 * @param sampleRate 采样率（Hz）
 * @param channels 声道数
 * @param bitsPerSample 位深（8/16/24）
 * @param signed 是否为有符号 PCM
 */

    @ConstructorProperties({ "sampleRate", "channels", "bitsPerSample", "signed" })
    public AudioFormat(int sampleRate, int channels, int bitsPerSample, boolean signed) {
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.bitsPerSample = bitsPerSample;
        this.signed = signed;
    }

    public int sampleRate() {
        return sampleRate;
    }

    public int channels() {
        return channels;
    }

    public int bitsPerSample() {
        return bitsPerSample;
    }

    public boolean signed() {
        return signed;
    }

    public int getSampleRate() {
        return sampleRate();
    }

    public int getChannels() {
        return channels();
    }

    public int getBitsPerSample() {
        return bitsPerSample();
    }

    public boolean getSigned() {
        return signed();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (Objects.isNull(obj) || getClass() != obj.getClass()) {
            return false;
        }
        AudioFormat other = (AudioFormat) obj;
        return this.sampleRate == other.sampleRate && this.channels == other.channels && this.bitsPerSample == other.bitsPerSample && this.signed == other.signed;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Integer.hashCode(sampleRate);
        result = 31 * result + Integer.hashCode(channels);
        result = 31 * result + Integer.hashCode(bitsPerSample);
        result = 31 * result + Boolean.hashCode(signed);
        return result;
    }

    @Override
    public String toString() {
        return "AudioFormat[sampleRate=" + sampleRate + ", channels=" + channels + ", bitsPerSample=" + bitsPerSample + ", signed=" + signed + "]";
    }
}
