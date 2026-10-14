package io.ddd4j.ai.extension.sst.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 文本转语音请求参数。
 *
 * @author ddd4j
 */
@Getter
@Setter
public class TTSDto {

    /**
     * 默认构造器（DTO 由调用方逐字段赋值）。
     */
    public TTSDto() {
    }

    private String text;

}
