package io.ddd4j.ai.core;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable AI invocation response.
 *
 * @param output   model or component output
 * @param metadata response metadata
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public record AiResponse(String output, Map<String, Object> metadata) {

    /**
     * 规范构造器：校验入参并复制元数据，保证响应对象不可变。
     *
     * @param output   模型或组件输出内容，不允许为 null
     * @param metadata 响应附加元数据，为 null 时视为空映射
     */
    public AiResponse {
        output = Objects.requireNonNull(output, "output");
        metadata = Objects.isNull(metadata) ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * 仅携带输出内容的快捷工厂方法。
     *
     * @param output 模型或组件输出内容
     * @return 元数据为空的响应实例
     */
    public static AiResponse of(String output) {
        return new AiResponse(output, Map.of());
    }
}
