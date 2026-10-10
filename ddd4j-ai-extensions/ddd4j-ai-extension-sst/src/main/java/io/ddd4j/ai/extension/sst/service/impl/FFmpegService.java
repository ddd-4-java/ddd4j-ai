package io.ddd4j.ai.extension.sst.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * FFmpeg 音频处理服务：通过 FFmpeg 命令行完成音频格式转换（wav 转 mp3、任意音频转 wav 等）。
 */
@Service
@Slf4j
public class FFmpegService {

    /**
     * 构造 FFmpegService 对象。
     */
    public FFmpegService() {
    }

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


    private String ffmpegBin = "ffmpeg";


    /**
     * 将 wav 格式的音频字节数组转换为 mp3 格式的音频字节数组（192kbps、44100Hz、单声道）。
     *
     * @param sourceAry 源 wav 音频字节数组
     * @return 转换后的 mp3 音频字节数组
     * @throws IOException          读写 FFmpeg 进程管道时发生 I/O 错误
     * @throws InterruptedException 等待 FFmpeg 进程结束时被中断
     */
    public byte[] convertWavToMp3FromByteAry(byte[] sourceAry) throws IOException, InterruptedException {
        // 构建FFmpeg命令
        String[] command = {
                ffmpegBin,
                "-i", "pipe:0",
                "-ab","192k",// 比特率
                "-ar", "44100", // 采样率
                "-ac", "1", // 单声道
                "-f", "mp3",// 输出格式
                "pipe:1"
        };

        // 执行FFmpeg命令
        return ffmpegConvertBytes(command, sourceAry);
    }


    /**
     * 将任意格式的音频字节数组转换为 wav 格式的音频字节数组（16000Hz、单声道）。
     *
     * @param sourceAry 源音频字节数组
     * @return 转换后的 wav 音频字节数组
     * @throws IOException          读写 FFmpeg 进程管道时发生 I/O 错误
     * @throws InterruptedException 等待 FFmpeg 进程结束时被中断
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
     * 将输入流中的音频转换为 wav 格式的音频字节数组（16000Hz、单声道）。
     *
     * @param inputStream 源音频输入流（读取完毕后自动关闭）
     * @return 转换后的 wav 音频字节数组
     * @throws IOException          读写 FFmpeg 进程管道时发生 I/O 错误
     * @throws InterruptedException 等待 FFmpeg 进程结束时被中断
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
