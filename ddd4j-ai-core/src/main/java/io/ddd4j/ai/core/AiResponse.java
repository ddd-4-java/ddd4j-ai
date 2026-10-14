package io.ddd4j.ai.core;

import java.beans.ConstructorProperties;

import java.util.Map;
import java.util.Objects;

/**
 * 不可变的 AI 调用响应：承载模型或组件输出与响应元数据。
 *
 * @param output   模型或组件输出内容
 * @param metadata 响应元数据
 * @author ddd4j
 */

public final class AiResponse {

    private static final long serialVersionUID = 0L;

    private final String output;

    private final Map<String, Object> metadata;

    /**
     * 紧凑构造器：校验入参并防御性复制元数据。
     *
     * @param output   模型或组件输出，不允许为 {@code null}
     * @param metadata 响应元数据，{@code null} 时以空 Map 替代并复制为不可变副本
     * @throws NullPointerException 当 {@code output} 为 {@code null} 时抛出
     */
    @ConstructorProperties({ "output", "metadata" })
    public AiResponse(String output, Map<String, Object> metadata) {
        output = Objects.requireNonNull(output, "output");
        metadata = Objects.isNull(metadata) ? Map.of() : Map.copyOf(metadata);
        this.output = output;
        this.metadata = metadata;
    }

    /**
     * 以输出内容创建不携带元数据的响应。
     *
     * @param output 模型或组件输出
     * @return 仅含输出、元数据为空的响应
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
