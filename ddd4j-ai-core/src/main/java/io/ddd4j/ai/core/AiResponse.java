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

    public AiResponse {
        output = Objects.requireNonNull(output, "output");
        metadata = Objects.isNull(metadata) ? Map.of() : Map.copyOf(metadata);
    }

    public static AiResponse of(String output) {
        return new AiResponse(output, Map.of());
    }
}
