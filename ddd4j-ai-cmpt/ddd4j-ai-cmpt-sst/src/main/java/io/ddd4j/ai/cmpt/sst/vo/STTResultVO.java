package io.ddd4j.ai.cmpt.sst.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class STTResultVO {

    private Integer status;

    private String msg;

    private String text;

    private String reason;

}
