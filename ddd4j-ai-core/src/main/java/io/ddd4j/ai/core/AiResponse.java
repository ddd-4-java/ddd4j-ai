package io.ddd4j.ai.core;

import java.beans.ConstructorProperties;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable AI invocation response.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */

public final class AiResponse {

    private static final long serialVersionUID = 0L;

    private final String output;

    private final Map<String, Object> metadata;

    /**
 * 规范构造器：校验入参并复制元数据，保证响应对象不可变。
 *
 * @param output 模型或组件输出内容，不允许为 null
 * @param metadata 响应附加元数据，为 null 时视为空映射
 */

    @ConstructorProperties({ "output", "metadata" })
    public AiResponse(String output, Map<String, Object> metadata) {
        output = Objects.requireNonNull(output, "output");
        metadata = Objects.isNull(metadata) ? Map.of() : Map.copyOf(metadata);
        this.output = output;
        this.metadata = metadata;
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

    public String output() {
        return output;
    }

    public Map<String, Object> metadata() {
        return metadata;
    }

    public String getOutput() {
        return output();
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
        AiResponse other = (AiResponse) obj;
        return Objects.equals(this.output, other.output) && Objects.equals(this.metadata, other.metadata);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Objects.hashCode(output);
        result = 31 * result + Objects.hashCode(metadata);
        return result;
    }

    @Override
    public String toString() {
        return "AiResponse[output=" + output + ", metadata=" + metadata + "]";
    }
}
