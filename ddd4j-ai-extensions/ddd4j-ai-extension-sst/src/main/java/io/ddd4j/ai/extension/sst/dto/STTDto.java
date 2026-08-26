package io.ddd4j.ai.extension.sst.dto;

import io.ddd4j.ai.extension.sst.enums.AVFormatEnums;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class STTDto {


    @NotNull
    private String channel;

    private String formatType = AVFormatEnums.Audio16Khz16KBitRateMonoWav.getKey();

    @NotNull
    private byte[] data;

}
