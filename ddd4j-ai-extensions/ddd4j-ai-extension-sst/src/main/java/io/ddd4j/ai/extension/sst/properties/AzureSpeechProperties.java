package io.ddd4j.ai.extension.sst.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Azure 语音服务配置属性：绑定 {@code azure.speech.*} 前缀下的订阅密钥、区域、音色与识别语言。
 */
@ConfigurationProperties(prefix = "azure.speech")
@Configuration
@Getter
@Setter
public class AzureSpeechProperties {

    /**
     * 构造 AzureSpeechProperties 配置（由 Spring 容器绑定属性时实例化）。
     */
    public AzureSpeechProperties() {
    }

    private String key;

    private String region;

    private String voiceName;

    private String recognitionLanguage;

}
