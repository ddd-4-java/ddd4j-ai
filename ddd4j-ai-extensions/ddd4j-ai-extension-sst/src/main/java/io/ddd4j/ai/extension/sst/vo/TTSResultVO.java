package io.ddd4j.ai.extension.sst.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * 文本转语音结果视图。
 *
 * @author ddd4j
 */
@Getter
@Setter
@Builder
public class TTSResultVO {

    /**
     * 默认构造器（配合 Lombok Builder 使用）。
     */
    public TTSResultVO() {
    }

    /**
     * 全参构造器（供 Lombok Builder 回填字段）。
     *
     * @param status 状态码
     * @param msg    结果消息
     * @param audio  合成音频字节流
     * @param reason 失败原因
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
