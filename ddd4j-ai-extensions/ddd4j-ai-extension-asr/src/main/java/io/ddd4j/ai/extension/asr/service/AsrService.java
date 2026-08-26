package io.ddd4j.ai.extension.asr.service;

import java.io.File;

/**
 * 语音转文本端口：离线 Whisper 转写字节流或文件。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public interface AsrService {

    /**
     * 转写音频字节（支持 WAV 头自动解析；非 WAV 按给定格式解释为 PCM）。
     *
     * @param audio  音频字节（WAV/PCM）
     * @param format 音频格式（WAV 头存在时忽略）
     * @return 识别文本
     * @throws Exception 引擎不可用 / 模型加载失败 / 音频非法
     */
    String transcribe(byte[] audio, AudioFormat format) throws Exception;

    /**
     * 转写音频文件。
     *
     * @param audio  音频文件
     * @param format 音频格式
     * @return 识别文本
     * @throws Exception 引擎不可用 / 模型加载失败 / 音频非法
     */
    String transcribe(File audio, AudioFormat format) throws Exception;
}
