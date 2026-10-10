package io.ddd4j.ai.extension.router.autoconfigure;

import io.ddd4j.ai.extension.router.properties.RouterProperties;
import io.ddd4j.ai.extension.router.service.ChatRouter;
import io.ddd4j.ai.extension.router.service.RoutingStrategy;
import io.ddd4j.ai.extension.router.service.impl.LatencyStrategy;
import io.ddd4j.ai.extension.router.service.impl.MultiModelChatRouter;
import io.ddd4j.ai.extension.router.service.impl.RoundRobinStrategy;
import io.ddd4j.ai.extension.router.service.impl.WeightedStrategy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Map;

/**
 * 多模型路由自动装配：业务侧注册多个 {@link ChatClient} bean（bean 名即模型 id），
 * 此处聚合为 ChatClient 池并按策略分发。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@AutoConfiguration(afterName = "org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration")
@ConditionalOnClass(ChatClient.class)
@ConditionalOnBean(ChatClient.class)
@ConditionalOnProperty(name = "ddd4j.ai.router.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(RouterProperties.class)
public class RouterAutoConfiguration {

    /**
     * 构造 RouterAutoConfiguration 自动装配类（由 Spring 容器实例化）。
     */
    public RouterAutoConfiguration() {
    }

    /**
     * 按配置注册路由策略实现（轮询 / 权重 / 延迟），业务侧可同类型 bean 覆盖。
     *
     * @param properties 路由配置（策略枚举与权重表）
     * @return 路由策略 bean
     */
    @Bean
    @ConditionalOnMissingBean(RoutingStrategy.class)
    public RoutingStrategy routingStrategy(RouterProperties properties) {
        return switch (properties.getStrategy()) {
            case WEIGHTED -> new WeightedStrategy(properties.getWeights());
            case LATENCY -> new LatencyStrategy();
            case ROUND_ROBIN -> new RoundRobinStrategy();
        };
    }

    /**
     * 注册多模型路由器：聚合容器内全部 {@link ChatClient} bean 并按策略分发。
     *
     * @param clients          模型 id → ChatClient 的映射
     * @param routingStrategy  分发策略
     * @return 聊天路由器 bean
     */
    @Bean
    @ConditionalOnMissingBean(ChatRouter.class)
    public ChatRouter chatRouter(Map<String, ChatClient> clients, RoutingStrategy routingStrategy) {
        return new MultiModelChatRouter(clients, routingStrategy);
    }
}
