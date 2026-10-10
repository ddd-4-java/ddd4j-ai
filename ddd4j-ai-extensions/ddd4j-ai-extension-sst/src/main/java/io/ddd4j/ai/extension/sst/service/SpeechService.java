package io.ddd4j.ai.extension.sst.service;


import io.ddd4j.ai.extension.sst.vo.TTSResultVO;

/**
 * 语音服务端口：提供文本转语音（TTS）与语音转文本（STT）能力，具体由实现类完成。
 *
 * @param <T> 语音配置类型（如 Azure 语音 SDK 的 {@code SpeechConfig}），由实现类决定
 */
public interface SpeechService<T> {

    /**
     * 文本转语音
     *
     * @param content  待合成为语音的文本内容
     * @param callback 合成结果回调（成功返回音频字节，失败或取消返回原因）
     * @throws Exception 语音合成或回调执行过程中发生的异常
     */
    void text2Voice(String content, SpeechServiceText2VoiceCallback callback) throws Exception;

    /**
     * 文本转语音：使用给定语音配置与文本合成音频，返回结果对象。
     *
     * @param speechConfigDto 语音配置，为 {@code null} 时使用服务默认配置
     * @param content         待合成为语音的文本内容
     * @return 合成结果对象，包含状态、消息与音频字节
     * @throws Exception 语音合成过程中发生的异常
     */
    TTSResultVO tts(T speechConfigDto, String content) throws Exception;

    /**
     * 语音转文本 从wav 文件
     *
     * @param wavFile   wav 音频文件路径
     * @param callback  识别结果回调（成功返回识别文本，失败或取消返回原因）
     * @throws Exception 音频读取或语音识别过程中发生的异常
     */
    void voice2TextFromWavFile(String wavFile, SpeechServiceVoice2TextCallback callback) throws Exception;

    /**
     * 语音转文本 从wav 字节流
     *
     * @param speechConfigDto 语音配置，为 {@code null} 时使用服务默认配置
     * @param wavFileBytes    wav 音频字节数组
     * @param callback        识别结果回调（成功返回识别文本，失败或取消返回原因）
     * @throws Exception 音频写入或语音识别过程中发生的异常
     */
    void voice2TextFromWavByteArray(T speechConfigDto, byte[] wavFileBytes, SpeechServiceVoice2TextCallback callback) throws Exception;

    /**
     * 语音转文本 从mp3 字节流
     *
     * @param speechConfigDto 语音配置，为 {@code null} 时使用服务默认配置
     * @param wavFileBytes    mp3 音频字节数组
     * @param callback        识别结果回调（成功返回识别文本，失败或取消返回原因）
     * @throws Exception 音频写入或语音识别过程中发生的异常
     */
    void voice2TextFromMp3ByteArray(T speechConfigDto, byte[] wavFileBytes, SpeechServiceVoice2TextCallback callback) throws Exception;

}
