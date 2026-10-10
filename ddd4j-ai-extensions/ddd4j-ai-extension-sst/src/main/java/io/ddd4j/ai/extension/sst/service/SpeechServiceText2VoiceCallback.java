package io.ddd4j.ai.extension.sst.service;

/**
 * 文本转声音回调
 */
public interface SpeechServiceText2VoiceCallback {

    /**
     * 取消
     *
     * @param reason 取消原因描述
     */
    void onCancel(String reason);

    /**
     * 转换成功
     *
     * @param audioData 合成成功的音频字节数据
     */
    void onSuccess(byte[] audioData);

    /**
     * 失败
     *
     * @param reason 失败原因描述
     */
    void onFail(String reason);
}
