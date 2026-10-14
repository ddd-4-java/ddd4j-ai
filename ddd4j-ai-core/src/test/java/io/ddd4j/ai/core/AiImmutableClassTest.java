package io.ddd4j.ai.core;

import org.junit.jupiter.api.Test;
import java.beans.ConstructorProperties;
import java.lang.reflect.Modifier;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

/** 验证普通不可变类迁移后的公共契约。 */
class AiImmutableClassTest {
    @Test
    void preservesComponentAndBeanAccessors() {
        AiRequest request = new AiRequest("prompt", Map.of("trace", "1"));
        assertThat(request.getInput()).isEqualTo(request.input());
        assertThat(request.getMetadata()).isEqualTo(request.metadata());
        AiResponse response = new AiResponse("answer", Map.of());
        assertThat(response.getOutput()).isEqualTo(response.output());
        assertThat(response.getMetadata()).isEqualTo(response.metadata());
    }

    @Test
    void preservesNamedConstructorAndValueHash() throws Exception {
        ConstructorProperties names = AiRequest.class.getConstructor(String.class, Map.class)
                .getAnnotation(ConstructorProperties.class);
        assertThat(names.value()).containsExactly("input", "metadata");
        AiRequest request = new AiRequest("prompt", Map.of());
        assertThat(request.hashCode()).isEqualTo(31 * "prompt".hashCode());
        assertThat(request.toString()).isEqualTo("AiRequest[input=prompt, metadata={}]");
        assertThat(AiRequest.class.isRecord()).isFalse();
        assertThat(Modifier.isFinal(AiRequest.class.getModifiers())).isTrue();
        assertThat(Modifier.isFinal(AiRequest.class.getDeclaredField("input").getModifiers())).isTrue();
    }
}
