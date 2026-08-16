package io.ddd4j.ai.core;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link AiResponse} 不可变契约测试（与 {@link AiRequestTest} 对称）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class AiResponseTest {

    @Nested
    class Of {

        @Test
        void ofEqualsFullConstructorWithEmptyMetadata() {
            assertThat(AiResponse.of("answer")).isEqualTo(new AiResponse("answer", Map.of()));
        }

        @Test
        void ofKeepsOutput() {
            assertThat(AiResponse.of("answer").output()).isEqualTo("answer");
        }
    }

    @Nested
    class NullSafety {

        @Test
        void nullOutputRejected() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> new AiResponse(null, Map.of()));
            assertThat(ex).hasMessage("output");
        }

        @Test
        void nullMetadataNormalizedToEmpty() {
            assertThat(new AiResponse("x", null).metadata()).isEmpty();
        }
    }

    @Nested
    class DefensiveCopy {

        @Test
        void mutatingSourceMapDoesNotAffectResponse() {
            Map<String, Object> source = new HashMap<>();
            source.put("model", "gpt-x");
            AiResponse response = new AiResponse("x", source);

            source.put("model", "changed");
            source.put("tokens", 42);

            assertThat(response.metadata())
                    .hasSize(1)
                    .containsEntry("model", "gpt-x");
        }

        @Test
        void exposedMetadataIsImmutable() {
            AiResponse response = AiResponse.of("x");
            assertThrows(UnsupportedOperationException.class,
                    () -> response.metadata().put("k", "v"));
        }
    }

    @Test
    void recordEquality() {
        Map<String, Object> metadata = Map.of("k", "v");
        assertThat(new AiResponse("a", metadata)).isEqualTo(new AiResponse("a", metadata));
        assertThat(new AiResponse("a", metadata)).isNotEqualTo(new AiResponse("b", metadata));
    }
}
