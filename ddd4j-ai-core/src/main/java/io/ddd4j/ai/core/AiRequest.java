package io.ddd4j.ai.core;

import java.beans.ConstructorProperties;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable AI invocation request.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */

public final class AiRequest {

    private static final long serialVersionUID = 0L;

    private final String input;

    private final Map<String, Object> metadata;

    /**
 * 规范构造器：校验入参并复制元数据，保证请求对象不可变。
 *
 * @param input 用户输入或规范化后的提示词，不允许为 null
 * @param metadata 调用方附加元数据，为 null 时视为空映射
 */

    @ConstructorProperties({ "input", "metadata" })
    public AiRequest(String input, Map<String, Object> metadata) {
        input = Objects.requireNonNull(input, "input");
        metadata = Objects.isNull(metadata) ? Map.of() : Map.copyOf(metadata);
        this.input = input;
        this.metadata = metadata;
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

    public String input() {
        return input;
    }

    public Map<String, Object> metadata() {
        return metadata;
    }

    public String getInput() {
        return input();
    }

    public Map<String, Object> getMetadata() {
        return metadata();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (Objects.isNull(obj) || getClass() != obj.getClass()) {
            return false;
        }
        AiRequest other = (AiRequest) obj;
        return Objects.equals(this.input, other.input) && Objects.equals(this.metadata, other.metadata);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Objects.hashCode(input);
        result = 31 * result + Objects.hashCode(metadata);
        return result;
    }

    @Override
    public String toString() {
        return "AiRequest[input=" + input + ", metadata=" + metadata + "]";
    }
}
