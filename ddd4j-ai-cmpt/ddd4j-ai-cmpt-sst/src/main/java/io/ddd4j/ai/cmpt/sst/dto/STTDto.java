package io.ddd4j.ai.cmpt.sst.dto;

import io.ddd4j.ai.cmpt.sst.enums.AVFormatEnums;
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
