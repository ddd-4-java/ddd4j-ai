package io.ddd4j.ai.extension.tts.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文本转语音组件配置（前缀 {@code ddd4j.ai.tts}）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = TtsProperties.PREFIX)
public class TtsProperties {

    /**
     * 构造 TtsProperties 配置（由 Spring 容器绑定属性时实例化）。
     */
    public TtsProperties() {
    }

    /** 配置前缀（{@code ddd4j.ai.tts}）。 */
    public static final String PREFIX = "ddd4j.ai.tts";

    /**
     * 是否启用文本转语音组件自动装配。
     */
    private boolean enabled = true;

    /**
     * 默认音色 shortName（Edge TTS 音色，如 zh-CN-XiaoxiaoNeural）。
     */
    private String defaultVoice = "zh-CN-XiaoxiaoNeural";
}
