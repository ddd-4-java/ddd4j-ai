package io.ddd4j.ai.cmpt.asr.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 语音识别组件配置（前缀 {@code ddd4j.ai.asr}）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = AsrProperties.PREFIX)
public class AsrProperties {

    public static final String PREFIX = "ddd4j.ai.asr";

    /**
     * 是否启用语音识别组件自动装配。
     */
    private boolean enabled = true;

    /**
     * Whisper 模型文件路径（ggml 格式，如 ggml-base.bin）。
     */
    private String modelPath = "models/ggml-base.bin";
}
