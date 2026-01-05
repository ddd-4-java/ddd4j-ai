package io.ddd4j.ai.cmpt.sst.service;

/**
 * 文本转声音回调
 */
public interface SpeechServiceText2VoiceCallback {

    /**
     * 取消
     * @param reason
     */
    void onCancel(String reason);

    /**
     * 转换成功
     * @param audioData
     */
    void onSuccess(byte[] audioData);

    /**
     * 失败
     * @param reason
     */
    void onFail(String reason);
}
