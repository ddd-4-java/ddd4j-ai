package io.ddd4j.ai.extension.rag.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 检索增强组件配置（前缀 {@code ddd4j.ai.rag}）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = RagProperties.PREFIX)
public class RagProperties {

    public static final String PREFIX = "ddd4j.ai.rag";

    /**
     * 是否启用 RAG 组件自动装配。
     */
    private boolean enabled = true;

    /**
     * 检索片段数上限。
     */
    private int topK = 4;

    /**
     * 摄取时是否启用 Token 分块。
     */
    private boolean chunking = true;

    /**
     * 增强查询 prompt 模板；占位符 {information}（检索内容）与 {question}（用户问题）。
     */
    private String promptTemplate = """
            请基于以下参考资料回答问题。若资料不足以回答，请明确说明。

            参考资料：
            {information}

            问题：{question}
            """;
}
