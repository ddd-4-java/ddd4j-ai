package io.ddd4j.ai.core;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link AiRequest} 不可变契约测试。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class AiRequestTest {

    @Nested
    class Of {

        @Test
        void ofEqualsFullConstructorWithEmptyMetadata() {
            assertThat(AiRequest.of("你好")).isEqualTo(new AiRequest("你好", Map.of()));
        }

        @Test
        void ofKeepsInput() {
            assertThat(AiRequest.of("prompt").input()).isEqualTo("prompt");
        }
    }

    @Nested
    class NullSafety {

        @Test
        void nullInputRejected() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> new AiRequest(null, Map.of()));
            assertThat(ex).hasMessage("input");
        }

        @Test
        void nullMetadataNormalizedToEmpty() {
            assertThat(new AiRequest("x", null).metadata()).isEmpty();
        }
    }

    @Nested
    class DefensiveCopy {

        @Test
        void mutatingSourceMapDoesNotAffectRequest() {
            Map<String, Object> source = new HashMap<>();
            source.put("traceId", "t-1");
            AiRequest request = new AiRequest("x", source);

            source.put("traceId", "t-2");
            source.put("extra", "leaked");

            assertThat(request.metadata())
                    .hasSize(1)
                    .containsEntry("traceId", "t-1");
        }

        @Test
        void exposedMetadataIsImmutable() {
            AiRequest request = AiRequest.of("x");
            assertThrows(UnsupportedOperationException.class,
                    () -> request.metadata().put("k", "v"));
        }
    }

    @Test
    void recordEquality() {
        Map<String, Object> metadata = Map.of("k", "v");
        assertThat(new AiRequest("a", metadata)).isEqualTo(new AiRequest("a", metadata));
        assertThat(new AiRequest("a", metadata)).isNotEqualTo(new AiRequest("b", metadata));
    }
}
