package io.ddd4j.ai.extension.sst.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 云语音服务配置（前缀 {@code azure.speech}）：密钥、区域、音色与识别语言。
 *
 * @author ddd4j
 */
@ConfigurationProperties(prefix = "azure.speech")
@Configuration
@Getter
@Setter
public class AzureSpeechProperties {

    /**
     * 默认构造器（配置属性类由 Spring 绑定）。
     */
    public AzureSpeechProperties() {
    }

    /** 订阅密钥。 */
    private String key;

    /** 服务区域。 */
    private String region;

    /** 合成音色。 */
    private String voiceName;

    /** 识别语言。 */
    private String recognitionLanguage;

}
