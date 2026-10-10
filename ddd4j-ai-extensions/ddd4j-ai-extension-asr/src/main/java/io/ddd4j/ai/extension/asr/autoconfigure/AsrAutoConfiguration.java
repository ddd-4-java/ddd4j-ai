package io.ddd4j.ai.extension.asr.autoconfigure;

import io.ddd4j.ai.extension.asr.properties.AsrProperties;
import io.ddd4j.ai.extension.asr.service.AsrService;
import io.ddd4j.ai.extension.asr.service.impl.WhisperCppAsrService;
import io.github.ggerganov.whispercpp.WhisperContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 语音识别自动装配：模型文件路径来自配置，引擎实例化懒加载于首次转写。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@AutoConfiguration
@ConditionalOnClass(WhisperContext.class)
@ConditionalOnProperty(name = "ddd4j.ai.asr.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AsrProperties.class)
public class AsrAutoConfiguration {

    /**
     * 构造 AsrAutoConfiguration 自动装配类（由 Spring 容器实例化）。
     */
    public AsrAutoConfiguration() {
    }

    /**
     * 构建 Whisper.cpp 语音识别服务（仅当业务侧未自行注入时生效）。
     *
     * @param properties 语音识别配置（模型文件路径等）
     * @return 基于 {@link WhisperCppAsrService} 的 {@link AsrService} 实例
     */
    @Bean
    @ConditionalOnMissingBean(AsrService.class)
    public AsrService asrService(AsrProperties properties) {
        return new WhisperCppAsrService(properties.getModelPath());
    }
}
