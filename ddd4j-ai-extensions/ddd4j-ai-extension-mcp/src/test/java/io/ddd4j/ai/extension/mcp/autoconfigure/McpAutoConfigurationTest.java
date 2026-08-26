package io.ddd4j.ai.extension.mcp.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link McpAutoConfiguration} 装配测试：客户端 bean 存在时激活，否则不注册。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class McpAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(McpAutoConfiguration.class));

    @Test
    void backsOffWhenDisabled() {
        runner.withPropertyValues("ddd4j.ai.mcp.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(McpAutoConfiguration.class));
    }
}
