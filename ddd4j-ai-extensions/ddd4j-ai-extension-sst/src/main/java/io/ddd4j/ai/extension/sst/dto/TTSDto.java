package io.ddd4j.ai.extension.sst.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 文本转语音（TTS）请求参数：待合成文本。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Getter
@Setter
public class TTSDto {

    /**
     * 构造 TTSDto 数据对象。
     */
    public TTSDto() {
    }

        private String text;

}
