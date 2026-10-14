package io.ddd4j.ai.extension.sst.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * FFmpeg 音频转码服务：通过管道（pipe:0/pipe:1）执行进程内字节流格式转换。
 *
 * @author ddd4j
 */
@Service
@Slf4j
public class FFmpegService {

    private String ffmpegBin = "ffmpeg";

    private byte[] ffmpegConvertBytes(String[] command, byte[] sourceAry) throws IOException, InterruptedException {
        log.info(StringUtils.join(command));
        // 执行FFmpeg命令
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(false);
        Process process = processBuilder.start();

        OutputStream processOutputStream = process.getOutputStream();
        processOutputStream.write(sourceAry, 0, sourceAry.length);
        processOutputStream.close();

        // 读取FFmpeg的标准输出
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (InputStream processInputStream = process.getInputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = processInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        // 等待FFmpeg进程完成
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg进程执行失败，退出代码：" + exitCode);
        }

        return outputStream.toByteArray();
    }

    private byte[] ffmpegConvertByInputStream(String[] command, InputStream inputStream) throws IOException, InterruptedException {
        // 执行FFmpeg命令
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        // 将输入流写入FFmpeg的标准输入
        try (InputStream in = inputStream) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                process.getOutputStream().write(buffer, 0, bytesRead);
            }
        }
        process.getOutputStream().close();

        // 读取FFmpeg的标准输出
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (InputStream processInputStream = process.getInputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = processInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        // 等待FFmpeg进程完成
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg进程执行失败，退出代码：" + exitCode);
        }

        return outputStream.toByteArray();
    }


    /**
     * 将 WAV 字节流转码为 MP3（192kbps / 44100Hz / 单声道）。
     *
     * @param sourceAry 源 WAV 字节流
     * @return 转码后的 MP3 字节流
     * @throws IOException 读写管道失败
     * @throws InterruptedException 等待进程被中断
     */
    public byte[] convertWavToMp3FromByteAry(byte[] sourceAry) throws IOException, InterruptedException {
        // 构建FFmpeg命令
        String[] command = {
                ffmpegBin,
                "-i", "pipe:0",
                "-ab", "192k",// 比特率
                "-ar", "44100", // 采样率
                "-ac", "1", // 单声道
                "-f", "mp3",// 输出格式
                "pipe:1"
        };

        // 执行FFmpeg命令
        return ffmpegConvertBytes(command, sourceAry);
    }


    /**
     * 将任意音频字节流转码为 WAV（16000Hz / 单声道）。
     *
     * @param sourceAry 源音频字节流
     * @return 转码后的 WAV 字节流
     * @throws IOException 读写管道失败
     * @throws InterruptedException 等待进程被中断
     */
    public byte[] convertAudioToWavFromByteAry(byte[] sourceAry) throws IOException, InterruptedException {
        // 构建FFmpeg命令
        String[] command = {
                ffmpegBin,
                "-i", "pipe:0",
                "-ac", "1",
                "-ar", "16000",
                "-f", "wav",
                "pipe:1"
        };

        // 执行FFmpeg命令
        return ffmpegConvertBytes(command, sourceAry);
    }


    /**
     * 将输入流音频转码为 WAV（16000Hz / 单声道）。
     *
     * @param inputStream 源音频输入流（方法内关闭）
     * @return 转码后的 WAV 字节流
     * @throws IOException 读写管道失败
     * @throws InterruptedException 等待进程被中断
     */
    public byte[] convertAudioToWavFromInputStream(InputStream inputStream) throws IOException, InterruptedException {
        // 构建FFmpeg命令
        String[] command = {
                ffmpegBin,
                "-i", "pipe:0",
                "-ac", "1",
                "-ar", "16000",
                "-f", "wav",
                "pipe:1"
        };

        return ffmpegConvertByInputStream(command, inputStream);
    }

}
