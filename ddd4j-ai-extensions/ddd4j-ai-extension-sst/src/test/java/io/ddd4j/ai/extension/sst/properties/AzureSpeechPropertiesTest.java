package io.ddd4j.ai.extension.sst.properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AzureSpeechProperties} 配置绑定测试：校验 {@code azure.speech.*} 前缀到字段的映射。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class AzureSpeechPropertiesTest {

    private org.springframework.boot.context.properties.bind.BindResult<AzureSpeechProperties> bind(Map<String, String> source) {
        return new Binder(new MapConfigurationPropertySource(source))
                .bind("azure.speech", Bindable.of(AzureSpeechProperties.class));
    }

    @Test
    void bindsAllFieldsFromKebabCaseKeys() {
        AzureSpeechProperties properties = bind(Map.of(
                "azure.speech.key", "secret-key",
                "azure.speech.region", "eastasia",
                "azure.speech.voice-name", "zh-CN-XiaoxiaoNeural",
                "azure.speech.recognition-language", "zh-CN")).get();

        assertThat(properties.getKey()).isEqualTo("secret-key");
        assertThat(properties.getRegion()).isEqualTo("eastasia");
        assertThat(properties.getVoiceName()).isEqualTo("zh-CN-XiaoxiaoNeural");
        assertThat(properties.getRecognitionLanguage()).isEqualTo("zh-CN");
    }

    @Test
    void missingPropertiesLeaveNulls() {
        AzureSpeechProperties properties = bind(Map.of("azure.speech.key", "only-key")).get();

        assertThat(properties.getKey()).isEqualTo("only-key");
        assertThat(properties.getRegion()).isNull();
        assertThat(properties.getVoiceName()).isNull();
        assertThat(properties.getRecognitionLanguage()).isNull();
    }

    @Test
    void emptySourceBindsNothing() {
        // Binder 语义：无任何属性命中时不绑定，由调用方给默认实例
        assertThat(bind(Map.of()).isBound()).isFalse();
    }
}
