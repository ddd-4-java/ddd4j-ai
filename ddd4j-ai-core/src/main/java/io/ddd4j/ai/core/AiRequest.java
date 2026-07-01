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

    public AiRequest {
        input = Objects.requireNonNull(input, "input");
        metadata = Objects.isNull(metadata) ? Map.of() : Map.copyOf(metadata);
    }

    public static AiRequest of(String input) {
        return new AiRequest(input, Map.of());
    }
}
