package io.ddd4j.ai.cmpt.asr.service.impl;

import io.ddd4j.ai.cmpt.asr.service.AudioFormat;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * WAV/PCM 转换工具：解析 WAV 头 → 混单声道 → 重采样到 16kHz → 归一化 float[-1,1]，
 * 供 Whisper（要求 16kHz 单声道 float 采样）消费。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public final class WavConverter {

    public static final int TARGET_SAMPLE_RATE = 16_000;

    private WavConverter() {
    }

    /**
     * 任意 WAV/PCM 字节 → 16kHz 单声道 float 采样。
     *
     * @param audio    WAV 字节（或裸 PCM）
     * @param fallback 非 WAV 时的格式假设
     */
    public static float[] toFloatMono16k(byte[] audio, AudioFormat fallback) {
        Header header = Header.parse(audio);
        int sampleRate = header != null ? header.sampleRate : fallback.sampleRate();
        int channels = header != null ? header.channels : fallback.channels();
        int bits = header != null ? header.bitsPerSample : fallback.bitsPerSample();
        byte[] pcm = header != null ? header.data : audio;
        float[] mono = toMonoFloat(pcm, channels, bits);
        return resample(mono, sampleRate, TARGET_SAMPLE_RATE);
    }

    static float[] toMonoFloat(byte[] pcm, int channels, int bits) {
        int bytesPerSample = bits / 8;
        int stride = bytesPerSample * channels;
        if (stride <= 0 || pcm.length < stride) {
            return new float[0];
        }
        int samples = pcm.length / stride;
        float[] result = new float[samples];
        for (int i = 0; i < samples; i++) {
            long sum = 0;
            for (int c = 0; c < channels; c++) {
                sum += sampleValue(pcm, i * stride + c * bytesPerSample, bits);
            }
            float average = (float) sum / channels;
            result[i] = switch (bits) {
                case 16 -> average / 32_768f;
                case 8 -> (average - 128) / 128f;
                case 24 -> average / 8_388_608f;
                default -> average;
            };
        }
        return result;
    }

    static float[] resample(float[] input, int fromRate, int toRate) {
        if (fromRate == toRate || input.length == 0) {
            return input;
        }
        int outLength = (int) ((long) input.length * toRate / fromRate);
        if (outLength == 0) {
            return new float[0];
        }
        float[] output = new float[outLength];
        double ratio = (double) input.length / outLength;
        for (int i = 0; i < outLength; i++) {
            double position = i * ratio;
            int index = (int) position;
            float fraction = (float) (position - index);
            float a = input[Math.min(index, input.length - 1)];
            float b = input[Math.min(index + 1, input.length - 1)];
            output[i] = a + (b - a) * fraction;
        }
        return output;
    }

    private static long sampleValue(byte[] pcm, int offset, int bits) {
        return switch (bits) {
            case 8 -> pcm[offset] & 0xFF;
            case 16 -> (short) ((pcm[offset] & 0xFF) | (pcm[offset + 1] << 8));
            case 24 -> ((pcm[offset] & 0xFF) | (pcm[offset + 1] & 0xFF) << 8 | pcm[offset + 2] << 16);
            default -> 0;
        };
    }

    /** WAV 头解析结果；非 WAV 返回 null。 */
    record Header(int sampleRate, int channels, int bitsPerSample, byte[] data) {

        static Header parse(byte[] bytes) {
            if (bytes.length < 44 || !"RIFF".equals(ascii(bytes, 0, 4)) || !"WAVE".equals(ascii(bytes, 8, 4))) {
                return null;
            }
            int position = 12;
            int sampleRate = 0;
            int channels = 0;
            int bits = 0;
            byte[] data = null;
            while (position + 8 <= bytes.length) {
                String id = ascii(bytes, position, 4);
                int size = littleEndianInt(bytes, position + 4);
                if ("fmt ".equals(id) && position + 24 <= bytes.length) {
                    channels = littleEndianShort(bytes, position + 10);
                    sampleRate = littleEndianInt(bytes, position + 12);
                    bits = littleEndianShort(bytes, position + 22);
                } else if ("data".equals(id)) {
                    data = Arrays.copyOfRange(bytes, position + 8,
                            Math.min(bytes.length, position + 8 + size));
                    break;
                }
                position += 8 + size + (size % 2);
            }
            if (data == null) {
                return null;
            }
            return new Header(sampleRate, channels, bits, data);
        }

        private static String ascii(byte[] bytes, int offset, int length) {
            return new String(bytes, offset, length, StandardCharsets.US_ASCII);
        }

        private static int littleEndianShort(byte[] bytes, int offset) {
            return (bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8);
        }

        private static int littleEndianInt(byte[] bytes, int offset) {
            return (bytes[offset] & 0xFF)
                    | ((bytes[offset + 1] & 0xFF) << 8)
                    | ((bytes[offset + 2] & 0xFF) << 16)
                    | ((bytes[offset + 3] & 0xFF) << 24);
        }
    }
}
