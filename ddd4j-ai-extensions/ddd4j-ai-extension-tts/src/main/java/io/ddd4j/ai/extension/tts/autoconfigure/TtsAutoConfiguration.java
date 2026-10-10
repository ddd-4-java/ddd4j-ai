package io.ddd4j.ai.extension.tts.autoconfigure;

import io.ddd4j.ai.extension.tts.properties.TtsProperties;
import io.ddd4j.ai.extension.tts.service.TtsService;
import io.ddd4j.ai.extension.tts.service.impl.EdgeTtsService;
import io.github.whitemagic2014.tts.TTS;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 文本转语音自动装配：默认音色来自配置，合成按需联网调用 Edge 服务。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@AutoConfiguration
@ConditionalOnClass(TTS.class)
@ConditionalOnProperty(name = "ddd4j.ai.tts.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(TtsProperties.class)
public class TtsAutoConfiguration {

    /**
     * 构造 TtsAutoConfiguration 自动装配类（由 Spring 容器实例化）。
     */
    public TtsAutoConfiguration() {
    }

    /**
     * 注册文本转语音服务：默认走 Edge TTS 实现，允许业务自定义 Bean 覆盖。
     *
     * @param properties 文本转语音组件配置（提供默认音色）
     * @return 文本转语音服务实例
     */
    @Bean
    @ConditionalOnMissingBean(TtsService.class)
    public TtsService ttsService(TtsProperties properties) {
        return new EdgeTtsService(properties.getDefaultVoice());
    }
}
