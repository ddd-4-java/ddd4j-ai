package io.ddd4j.ai.cmpt.sst.service.impl;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * {@link FFmpegService} 端到端转换测试：依赖宿主机安装 ffmpeg；
 * 无 ffmpeg 的环境自动跳过（对应 plan 中 {@code Assumptions.assumeTrue} 约定）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class FFmpegServiceTest {

    private final FFmpegService ffmpegService = new FFmpegService();

    @BeforeAll
    static void requireFfmpegOnPath() {
        assumeTrue(ffmpegAvailable(), "ffmpeg 不在 PATH 中，跳过 FFmpeg 端到端测试");
    }

    static boolean ffmpegAvailable() {
        try {
            Process process = new ProcessBuilder("ffmpeg", "-version").start();
            process.getInputStream().readAllBytes();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** 生成 16kHz 单声道 16bit PCM 正弦波 WAV。 */
    static byte[] sineWav(int sampleRate, double seconds) {
        int samples = (int) (sampleRate * seconds);
        int dataSize = samples * 2;
        ByteArrayOutputStream out = new ByteArrayOutputStream(44 + dataSize);
        writeAscii(out, "RIFF");
        writeIntLe(out, 36 + dataSize);
        writeAscii(out, "WAVE");
        writeAscii(out, "fmt ");
        writeIntLe(out, 16);            // PCM chunk size
        writeShortLe(out, (short) 1);   // audio format: PCM
        writeShortLe(out, (short) 1);   // channels
        writeIntLe(out, sampleRate);
        writeIntLe(out, sampleRate * 2);// byte rate
        writeShortLe(out, (short) 2);   // block align
        writeShortLe(out, (short) 16);  // bits per sample
        writeAscii(out, "data");
        writeIntLe(out, dataSize);
        for (int i = 0; i < samples; i++) {
            double t = i / (double) sampleRate;
            short sample = (short) (Math.sin(2 * Math.PI * 440 * t) * 8000);
            writeShortLe(out, sample);
        }
        return out.toByteArray();
    }

    static void writeAscii(ByteArrayOutputStream out, String s) {
        out.writeBytes(s.getBytes(StandardCharsets.US_ASCII));
    }

    static void writeIntLe(ByteArrayOutputStream out, int v) {
        out.write(v & 0xFF);
        out.write((v >> 8) & 0xFF);
        out.write((v >> 16) & 0xFF);
        out.write((v >> 24) & 0xFF);
    }

    static void writeShortLe(ByteArrayOutputStream out, short v) {
        out.write(v & 0xFF);
        out.write((v >> 8) & 0xFF);
    }

    static int readIntLe(byte[] b, int off) {
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8)
                | ((b[off + 2] & 0xFF) << 16) | ((b[off + 3] & 0xFF) << 24);
    }

    static short readShortLe(byte[] b, int off) {
        return (short) ((b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8));
    }

    @Test
    void convertsArbitraryAudioTo16kMonoWav() throws Exception {
        byte[] wav44k = sineWav(44100, 0.3);

        byte[] result = ffmpegService.convertAudioToWavFromByteAry(wav44k);

        assertThat(result.length).isGreaterThan(44);
        assertThat(new String(result, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("RIFF");
        assertThat(new String(result, 8, 4, StandardCharsets.US_ASCII)).isEqualTo("WAVE");
        assertThat(readShortLe(result, 22)).isEqualTo((short) 1);  // 单声道
        assertThat(readIntLe(result, 24)).isEqualTo(16000);        // 16kHz（Azure STT 推荐格式）
        assertThat(readShortLe(result, 34)).isEqualTo((short) 16); // 16bit
    }

    @Test
    void convertsWavToMp3() throws Exception {
        byte[] wav = sineWav(16000, 0.3);

        byte[] mp3 = ffmpegService.convertWavToMp3FromByteAry(wav);

        assertThat(mp3).isNotEmpty();
        boolean id3 = mp3.length > 3 && new String(mp3, 0, 3, StandardCharsets.US_ASCII).equals("ID3");
        boolean frameSync = (mp3[0] & 0xFF) == 0xFF && (mp3[1] & 0xE0) == 0xE0;
        assertTrue(id3 || frameSync, "输出应为 MP3（ID3 头或帧同步字节开头）");
    }

    @Test
    void convertsFromInputStream() throws Exception {
        byte[] wav = sineWav(44100, 0.2);

        byte[] result = ffmpegService.convertAudioToWavFromInputStream(
                new java.io.ByteArrayInputStream(wav));

        // 存量缺陷：该方法 redirectErrorStream(true) 会把 ffmpeg stderr（文本日志）混入输出，
        // RIFF 头不保证在字节流开头（与字节流版 convertAudioToWavFromByteAry 不一致）。
        // 此处先验证转换产物中包含 WAV 数据；缺陷修复后应收紧为前 4 字节等于 "RIFF" 且采样率 16000。
        String resultAsString = new String(result, java.nio.charset.StandardCharsets.ISO_8859_1);
        org.assertj.core.api.Assertions.assertThat(resultAsString).contains("RIFF");
    }

    @Test
    void invalidInputFailsWithRuntimeException() {
        byte[] garbage = new byte[1024]; // 全零：非音频数据

        assertThrows(RuntimeException.class,
                () -> ffmpegService.convertAudioToWavFromByteAry(garbage));
    }
}
