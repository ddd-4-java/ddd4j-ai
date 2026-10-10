package io.ddd4j.ai.extension.sst.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * 语音转文本（STT）结果视图对象：封装识别状态、消息、识别文本与失败原因。
 */
@Getter
@Setter
@Builder
public class STTResultVO {

    /**
     * 构造 STTResultVO 数据对象。
     */
    public STTResultVO() {
    }

    /**
     * 构造完整的语音识别结果对象（同时供 {@code @Builder} 的 build() 使用）。
     *
     * @param status 识别状态码
     * @param msg    状态消息
     * @param text   识别出的文本
     * @param reason 失败原因（成功时为空）
     */
    public STTResultVO(Integer status, String msg, String text, String reason) {
        this.status = status;
        this.msg = msg;
        this.text = text;
        this.reason = reason;
    }

    private Integer status;

    private String msg;

    private String text;

    private String reason;

}
