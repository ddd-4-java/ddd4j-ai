package io.ddd4j.ai.extension.sst.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * 语音转文本结果视图。
 *
 * @author ddd4j
 */
@Getter
@Setter
@Builder
public class STTResultVO {

    /**
     * 默认构造器（配合 Lombok Builder 使用）。
     */
    public STTResultVO() {
    }

    /**
     * 全参构造器（供 Lombok Builder 回填字段）。
     *
     * @param status 状态码
     * @param msg    结果消息
     * @param text   识别出的文本
     * @param reason 失败原因
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
