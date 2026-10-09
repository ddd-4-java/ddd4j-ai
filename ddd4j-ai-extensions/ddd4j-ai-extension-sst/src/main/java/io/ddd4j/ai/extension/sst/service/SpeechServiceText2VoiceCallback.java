package io.ddd4j.ai.extension.sst.service;

/**
 * 文本转声音回调
 */
public interface SpeechServiceText2VoiceCallback {

    /**
     * 合成被取消。
     *
     * @param reason 取消原因
     */
    void onCancel(String reason);

    /**
     * 合成成功。
     *
     * @param audioData 合成音频字节流
     */
    void onSuccess(byte[] audioData);

    /**
     * 合成失败。
     *
     * @param reason 失败原因
     */
    void onFail(String reason);
}
