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
 * @param input user input or normalized prompt
 * @param metadata caller-provided metadata
 */

    @ConstructorProperties({ "input", "metadata" })
    public AiRequest(String input, Map<String, Object> metadata) {
        input = Objects.requireNonNull(input, "input");
        metadata = Objects.isNull(metadata) ? Map.of() : Map.copyOf(metadata);
        this.input = input;
        this.metadata = metadata;
    }

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
