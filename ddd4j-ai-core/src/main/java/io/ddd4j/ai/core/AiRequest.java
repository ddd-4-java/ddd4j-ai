package io.ddd4j.ai.core;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable AI invocation request.
 *
 * @param input    user input or normalized prompt
 * @param metadata caller-provided metadata
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public record AiRequest(String input, Map<String, Object> metadata) {

    /**
     * 规范构造器：校验入参并复制元数据，保证请求对象不可变。
     *
     * @param input    用户输入或规范化后的提示词，不允许为 null
     * @param metadata 调用方附加元数据，为 null 时视为空映射
     */
    public AiRequest {
        input = Objects.requireNonNull(input, "input");
        metadata = Objects.isNull(metadata) ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * 仅携带输入内容的快捷工厂方法。
     *
     * @param input 用户输入或规范化后的提示词
     * @return 元数据为空的请求实例
     */
    public static AiRequest of(String input) {
        return new AiRequest(input, Map.of());
    }
}
