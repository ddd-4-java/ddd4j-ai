package io.ddd4j.ai.extension.sst.service;

/**
 * 声音转文本
 */
public interface SpeechServiceVoice2TextCallback {

    /**
     * 识别失败。
     *
     * @param reason 失败原因
     */
    void onFail(String reason);

    /**
     * 识别被取消。
     *
     * @param reason 取消原因
     */
    void onCancel(String reason);

    /**
     * 识别成功。
     *
     * @param text 识别出的文本
     */
    void onSuccess(String text);

}
