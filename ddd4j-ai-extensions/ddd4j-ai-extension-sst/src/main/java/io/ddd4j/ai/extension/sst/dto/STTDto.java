package io.ddd4j.ai.extension.sst.dto;

import io.ddd4j.ai.extension.sst.enums.AVFormatEnums;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 语音转文本请求参数。
 *
 * @author ddd4j
 */
@Getter
@Setter
public class STTDto {

    /**
     * 默认构造器（DTO 由调用方逐字段赋值）。
     */
    public STTDto() {
    }


    @NotNull
    private String channel;

    private String formatType = AVFormatEnums.Audio16Khz16KBitRateMonoWav.getKey();

    @NotNull
    private byte[] data;

}
