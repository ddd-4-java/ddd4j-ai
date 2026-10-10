package io.ddd4j.ai.extension.sst.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * 文本转语音（TTS）结果视图对象：封装合成状态、消息、音频字节与失败原因。
 */
@Getter
@Setter
@Builder
public class TTSResultVO {

    /**
     * 构造 TTSResultVO 数据对象。
     */
    public TTSResultVO() {
    }

    /**
     * 构造完整的语音合成结果对象（同时供 {@code @Builder} 的 build() 使用）。
     *
     * @param status 合成状态码
     * @param msg    状态消息
     * @param audio  合成的音频字节
     * @param reason 失败原因（成功时为空）
     */
    public TTSResultVO(Integer status, String msg, byte[] audio, String reason) {
        this.status = status;
        this.msg = msg;
        this.audio = audio;
        this.reason = reason;
    }

    private Integer status;

    private String msg;

    private byte[] audio;

    private String reason;

}
