package io.ddd4j.ai.core;

import java.util.Map;
import java.util.Objects;

/**
 * 不可变的 AI 调用请求：承载用户输入与调用方元数据。
 *
 * @param input    用户输入或规范化后的提示词
 * @param metadata 调用方自定义元数据
 * @author ddd4j
 */
public record AiRequest(String input, Map<String, Object> metadata) {

    /**
     * 紧凑构造器：校验入参并防御性复制元数据。
     *
     * @param input    用户输入，不允许为 {@code null}
     * @param metadata 调用方元数据，{@code null} 时以空 Map 替代并复制为不可变副本
     * @throws NullPointerException 当 {@code input} 为 {@code null} 时抛出
     */
    public AiRequest {
        input = Objects.requireNonNull(input, "input");
        metadata = Objects.isNull(metadata) ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * 以用户输入创建不携带元数据的请求。
     *
     * @param input 用户输入
     * @return 仅含输入、元数据为空的请求
     */
    public static AiRequest of(String input) {
        return new AiRequest(input, Map.of());
    }
}
