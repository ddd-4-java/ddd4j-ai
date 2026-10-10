package io.ddd4j.ai.extension.sst.dto;

import io.ddd4j.ai.extension.sst.enums.AVFormatEnums;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 语音转文本（STT）请求参数：通道、音频格式 key 与二进制音频数据。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Getter
@Setter
public class STTDto {

    /**
     * 构造 STTDto 数据对象。
     */
    public STTDto() {
    }


    @NotNull
    private String channel;

    private String formatType = AVFormatEnums.Audio16Khz16KBitRateMonoWav.getKey();

    @NotNull
    private byte[] data;

}
