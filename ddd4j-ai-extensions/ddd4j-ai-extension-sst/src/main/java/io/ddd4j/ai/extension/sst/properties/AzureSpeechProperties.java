package io.ddd4j.ai.extension.sst.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "azure.speech")
@Configuration
@Getter
@Setter
public class AzureSpeechProperties {

    private String key;

    private String region;

    private String voiceName;

    private String recognitionLanguage;

}
