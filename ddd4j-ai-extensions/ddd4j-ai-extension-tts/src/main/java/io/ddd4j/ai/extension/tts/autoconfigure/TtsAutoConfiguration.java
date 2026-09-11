package io.ddd4j.ai.extension.tts.autoconfigure;

import io.ddd4j.ai.extension.tts.metrics.TtsMetrics;
import io.ddd4j.ai.extension.tts.properties.TtsProperties;
import io.ddd4j.ai.extension.tts.router.TtsRouter;
import io.ddd4j.ai.extension.tts.router.impl.FallbackTtsRouter;
import io.ddd4j.ai.extension.tts.service.TtsService;
import io.ddd4j.ai.extension.tts.service.impl.DashScopeRealtimeClient;
import io.ddd4j.ai.extension.tts.service.impl.DashScopeRealtimeTtsService;
import io.ddd4j.ai.extension.tts.service.impl.EdgeTtsService;
import io.github.whitemagic2014.tts.TTS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 文本转语音自动装配：
 * <ul>
 *   <li>Edge TTS 后端（whitemagic 库，免费在线，无需 API key）。</li>
 *   <li>DashScope Qwen3-TTS Realtime 后端（dashscope-sdk-java，需 API key，TTFA ~97ms）。</li>
 *   <li>{@link TtsRouter}：按 {@code primary} 配置顺序装配 primary → fallback 链路，
 *       主后端失败时自动降级。</li>
 *   <li>{@link TtsMetrics}：TTFA 自维护采样器（零依赖，Logger 输出）。</li>
 * </ul>
 *
 * <p>对业务方暴露的 Bean：
 * <ul>
 *   <li>{@link TtsRouter}（统一入口，推荐）</li>
 *   <li>{@link TtsService}（named：{@code edgeTtsService} / {@code dashScopeRealtimeTtsService}，按需注入）</li>
 *   <li>{@link TtsMetrics}（可选注入用于查询/重置）</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@AutoConfiguration
@ConditionalOnClass(TTS.class)
@ConditionalOnProperty(name = "ddd4j.ai.tts.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(TtsProperties.class)
public class TtsAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(TtsAutoConfiguration.class);

    /**
     * Edge TTS 后端 Bean（always-on 默认后端）。
     */
    @Bean(name = "edgeTtsService")
    @ConditionalOnMissingBean(name = "edgeTtsService")
    @ConditionalOnProperty(name = "ddd4j.ai.tts.edge.enabled", havingValue = "true", matchIfMissing = true)
    public TtsService edgeTtsService(TtsProperties properties) {
        return new EdgeTtsService(properties.getEdge().getDefaultVoice());
    }

    /**
     * DashScope Qwen3-TTS Realtime 后端 Bean（仅在 api-key 存在时启用）。
     *
     * <p>使用 {@code @ConditionalOnExpression} 确保无 api-key 时整个 Bean 不被创建，
     * 避免 Spring 不接受 {@code @Bean} 返回 null 的限制。
     */
    @Bean(name = "dashScopeRealtimeTtsService")
    @ConditionalOnMissingBean(name = "dashScopeRealtimeTtsService")
    @ConditionalOnExpression("'${ddd4j.ai.tts.dashscope.enabled:true}'.equals('true') "
            + "and '${ddd4j.ai.tts.dashscope.api-key:}' != ''")
    public TtsService dashScopeRealtimeTtsService(TtsProperties properties,
                                                 org.springframework.beans.factory.ObjectProvider<TtsMetrics> metricsProvider) {
        TtsMetrics metrics = metricsProvider.getIfAvailable();
        if (metrics == null) {
            metrics = new TtsMetrics();
        }
        return DashScopeRealtimeTtsService.create(
                properties.getDashscope().getApiKey(),
                properties.getDashscope().getModel(),
                properties.getDashscope().getVoice(),
                metrics);
    }

    /**
     * TTFA 指标 Bean（共享）。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "ddd4j.ai.tts.metrics.enabled", havingValue = "true", matchIfMissing = true)
    public TtsMetrics ttsMetrics() {
        return new TtsMetrics();
    }

    /**
     * 多后端路由器 Bean：按 primary 顺序装配 fallback 链路，主后端失败时自动降级。
     */
    @Bean
    @ConditionalOnMissingBean(TtsRouter.class)
    public TtsRouter ttsRouter(Map<String, TtsService> ttsServiceBeans, TtsProperties properties) {
        Map<String, TtsService> chain = buildBackendChain(ttsServiceBeans, properties);
        if (chain.isEmpty()) {
            // 退而求其次：让 Spring 抛 NoSuchBean 错误，业务方看到明确报错
            throw new IllegalStateException("未发现任何 TTS 后端，请检查 ddd4j.ai.tts.edge / dashscope 配置");
        }
        log.info("TTS router chain: {}", chain.keySet());
        return new FallbackTtsRouter(chain);
    }

    /**
     * 按 {@code primary} 配置构造 ordered backend 链路：primary 在前，其余 fallback 按顺序追加。
     *
     * <p>若 primary 配置的后端不可用（如 DashScope 缺 api-key），自动降级到剩下唯一可用的后端，
     * 而不是抛错；只有全部后端都不可用时才报错。
     */
    private static Map<String, TtsService> buildBackendChain(Map<String, TtsService> ttsServiceBeans,
                                                              TtsProperties properties) {
        Map<String, TtsService> chain = new LinkedHashMap<>();

        String primaryName = properties.resolvedPrimary() == TtsProperties.PrimaryType.DASHSCOPE
                ? "dashScopeRealtimeTtsService"
                : "edgeTtsService";
        String fallbackName = primaryName.equals("edgeTtsService")
                ? "dashScopeRealtimeTtsService"
                : "edgeTtsService";

        TtsService primary = ttsServiceBeans.get(primaryName);
        if (primary != null) {
            chain.put(primaryName, primary);
        }
        TtsService fallback = ttsServiceBeans.get(fallbackName);
        if (fallback != null && fallback != primary) {
            chain.put(fallbackName, fallback);
        }
        return chain;
    }
}