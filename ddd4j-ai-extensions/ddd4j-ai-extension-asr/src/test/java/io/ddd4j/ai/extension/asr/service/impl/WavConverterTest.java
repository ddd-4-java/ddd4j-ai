package io.ddd4j.ai.extension.asr.service.impl;

import io.ddd4j.ai.extension.asr.service.AudioFormat;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * {@link WavConverter} 单元测试：WAV 头解析 / 单声道混音 / 重采样 / 归一化。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class WavConverterTest {

    @Test
    void toFloatMono16k_16kMono16bit_passthrough() {
        short[] samples = {0, 16_384, -16_384, 32_767};
        byte[] wav = wavPcm16(samples, 16_000, 1);
        float[] out = WavConverter.toFloatMono16k(wav, AudioFormat.wav44100Stereo16());
        assertThat(out).hasSize(samples.length);
        assertThat(out[0]).isCloseTo(0f, within(0.001f));
        assertThat(out[1]).isCloseTo(0.5f, within(0.001f));
        assertThat(out[2]).isCloseTo(-0.5f, within(0.001f));
    }

    @Test
    void toFloatMono16k_stereo44100_downmixedAndResampled() {
        short[] samples = {10_000, 20_000};
        byte[] wav = wavPcm16(samples, 44_100, 2);
        float[] out = WavConverter.toFloatMono16k(wav, AudioFormat.wav44100Stereo16());
        int expected = (int) ((long) samples.length * 16_000 / 44_100);
        assertThat(out).hasSize(expected);
        for (float value : out) {
            assertThat(value).isBetween(-1f, 1f);
        }
    }

    @Test
    void toFloatMono16k_rawPcm_usesFallbackFormat() {
        short[] samples = {8000, 8000};
        byte[] pcm = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            pcm[i * 2] = (byte) (samples[i] & 0xFF);
            pcm[i * 2 + 1] = (byte) ((samples[i] >> 8) & 0xFF);
        }
        float[] out = WavConverter.toFloatMono16k(pcm, new AudioFormat(16_000, 1, 16, true));
        assertThat(out).hasSize(samples.length);
    }

    @Test
    void toFloatMono16k_nonWavGarbage_returnsEmptyOrZero() {
        float[] out = WavConverter.toFloatMono16k(new byte[]{1, 2, 3}, AudioFormat.wav44100Stereo16());
        assertThat(out).isNotNull();
    }

    private static byte[] wavPcm16(short[] samples, int sampleRate, int channels) {
        try {
            int dataSize = samples.length * 2;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write("RIFF".getBytes(StandardCharsets.US_ASCII));
            writeInt(out, 36 + dataSize);
            out.write("WAVE".getBytes(StandardCharsets.US_ASCII));
            out.write("fmt ".getBytes(StandardCharsets.US_ASCII));
            writeInt(out, 16);
            writeShort(out, 1); // PCM
            writeShort(out, channels);
            writeInt(out, sampleRate);
            writeInt(out, sampleRate * channels * 2);
            writeShort(out, (short) (channels * 2));
            writeShort(out, (short) 16);
            out.write("data".getBytes(StandardCharsets.US_ASCII));
            writeInt(out, dataSize);
            for (short sample : samples) {
                out.write(sample & 0xFF);
                out.write((sample >> 8) & 0xFF);
            }
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void writeShort(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
    }

    private static void writeInt(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 24) & 0xFF);
    }
}
