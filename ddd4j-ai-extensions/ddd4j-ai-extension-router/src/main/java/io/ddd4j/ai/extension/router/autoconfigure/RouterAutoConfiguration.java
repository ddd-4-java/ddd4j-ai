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

    @Bean
    @ConditionalOnMissingBean(RoutingStrategy.class)
    public RoutingStrategy routingStrategy(RouterProperties properties) {
        return switch (properties.getStrategy()) {
            case WEIGHTED -> new WeightedStrategy(properties.getWeights());
            case LATENCY -> new LatencyStrategy();
            case ROUND_ROBIN -> new RoundRobinStrategy();
        };
    }

    @Bean
    @ConditionalOnMissingBean(ChatRouter.class)
    public ChatRouter chatRouter(Map<String, ChatClient> clients, RoutingStrategy routingStrategy) {
        return new MultiModelChatRouter(clients, routingStrategy);
    }
}
