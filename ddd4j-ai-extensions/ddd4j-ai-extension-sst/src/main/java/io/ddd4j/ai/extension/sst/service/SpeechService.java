package io.ddd4j.ai.extension.sst.service;


import io.ddd4j.ai.extension.sst.vo.TTSResultVO;

public interface SpeechService<T> {

    /**
     * 文本转语音
     *
     * @param content
     * @param callback
     * @throws Exception
     */
    void text2Voice(String content, SpeechServiceText2VoiceCallback callback) throws Exception;

    TTSResultVO tts(T speechConfigDto, String content) throws Exception;

    /**
     * 语音转文本 从wav 文件
     *
     * @param wavFile
     * @param callback
     * @throws Exception
     */
    void voice2TextFromWavFile(String wavFile, SpeechServiceVoice2TextCallback callback) throws Exception;

    /**
     * 语音转文本 从wav 字节流
     *
     * @param speechConfigDto
     * @param wavFileBytes
     * @param callback
     * @throws Exception
     */
    void voice2TextFromWavByteArray(T speechConfigDto, byte[] wavFileBytes, SpeechServiceVoice2TextCallback callback) throws Exception;

    void voice2TextFromMp3ByteArray(T speechConfigDto, byte[] wavFileBytes, SpeechServiceVoice2TextCallback callback) throws Exception;

}
