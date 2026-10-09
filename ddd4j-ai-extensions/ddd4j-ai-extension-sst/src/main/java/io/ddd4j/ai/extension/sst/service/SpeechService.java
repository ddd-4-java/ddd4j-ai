package io.ddd4j.ai.extension.sst.service;


import io.ddd4j.ai.extension.sst.vo.TTSResultVO;

/**
 * 语音合成与识别端口：文本转语音、语音转文本（WAV/MP3）。
 *
 * @param <T> 渠道专属配置类型（如语音配置对象）
 * @author ddd4j
 */
public interface SpeechService<T> {

    /**
     * 文本转语音。
     *
     * @param content  待合成文本
     * @param callback 合成结果回调
     * @throws Exception 合成过程任意异常
     */
    void text2Voice(String content, SpeechServiceText2VoiceCallback callback) throws Exception;

    /**
     * 文本转语音（同步返回结果）。
     *
     * @param speechConfigDto 渠道语音配置
     * @param content         待合成文本
     * @return 合成结果（状态、音频与失败原因）
     * @throws Exception 合成过程任意异常
     */
    TTSResultVO tts(T speechConfigDto, String content) throws Exception;

    /**
     * 语音转文本（从 WAV 文件路径）。
     *
     * @param wavFile  WAV 文件路径
     * @param callback 识别结果回调
     * @throws Exception 识别过程任意异常
     */
    void voice2TextFromWavFile(String wavFile, SpeechServiceVoice2TextCallback callback) throws Exception;

    /**
     * 语音转文本（从 WAV 字节流）。
     *
     * @param speechConfigDto 渠道语音配置
     * @param wavFileBytes    WAV 字节流
     * @param callback        识别结果回调
     * @throws Exception 识别过程任意异常
     */
    void voice2TextFromWavByteArray(T speechConfigDto, byte[] wavFileBytes, SpeechServiceVoice2TextCallback callback) throws Exception;

    /**
     * 语音转文本（从 MP3 字节流）。
     *
     * @param speechConfigDto 渠道语音配置
     * @param wavFileBytes    MP3 字节流
     * @param callback        识别结果回调
     * @throws Exception 识别过程任意异常
     */
    void voice2TextFromMp3ByteArray(T speechConfigDto, byte[] wavFileBytes, SpeechServiceVoice2TextCallback callback) throws Exception;

}
