package io.ddd4j.ai.extension.sst.service;

/**
 * 声音转文本
 */
public interface SpeechServiceVoice2TextCallback {

    /**
     * 错误
     * @param reason
     */
    void onFail(String reason);

    /**
     * 取消
     * @param reason
     */
    void onCancel(String reason);

    /**
     * 成功返回
     * @param text
     */
    void onSuccess(String text);

}
