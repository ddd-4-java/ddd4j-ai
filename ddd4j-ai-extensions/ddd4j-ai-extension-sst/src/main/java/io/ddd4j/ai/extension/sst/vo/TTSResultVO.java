package io.ddd4j.ai.extension.sst.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TTSResultVO {

    private Integer status;

    private String msg;

    private byte[] audio;

    private String reason;

}
