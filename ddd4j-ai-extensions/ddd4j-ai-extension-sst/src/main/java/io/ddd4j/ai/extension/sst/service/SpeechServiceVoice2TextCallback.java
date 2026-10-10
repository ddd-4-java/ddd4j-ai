package io.ddd4j.ai.extension.sst.service;

/**
 * 声音转文本
 */
public interface SpeechServiceVoice2TextCallback {

    /**
     * 错误
     *
     * @param reason 错误原因描述
     */
    void onFail(String reason);

    /**
     * 取消
     *
     * @param reason 取消原因描述
     */
    void onCancel(String reason);

    /**
     * 成功返回
     *
     * @param text 识别成功的文本内容
     */
    void onSuccess(String text);

}
