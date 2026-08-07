package io.ddd4j.ai.cmpt.sst.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Service
@Slf4j
public class FFmpegService {

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
