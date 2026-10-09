package io.ddd4j.ai.extension.chat.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 对话组件配置（前缀 {@code ddd4j.ai.chat}）。
 *
 * @author ddd4j
 */
@Getter
@Setter
@ConfigurationProperties(prefix = ChatProperties.PREFIX)
public class ChatProperties {

    /**
     * 默认构造器（配置属性类由 Spring 绑定）。
     */
    public ChatProperties() {
    }

    /** 配置属性前缀。 */
    public static final String PREFIX = "ddd4j.ai.chat";

    /**
     * 是否启用对话组件自动装配。
     */
    private boolean enabled = true;

    /**
     * 默认系统提示词；为空则不注入 system 消息。
     */
    private String defaultSystemPrompt;
}
