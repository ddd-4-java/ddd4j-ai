package io.ddd4j.ai.extension.embedding.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link EmbeddingModelAdapter} 单元测试：mock EmbeddingModel，验证分片、顺序与维度透传。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class EmbeddingModelAdapterTest {

    private final EmbeddingModel model = mock(EmbeddingModel.class);

    @Test
    void singleEmbedDelegates() {
        when(model.embed("你好")).thenReturn(new float[]{0.1f, 0.2f});

        assertThat(new EmbeddingModelAdapter(model, 32).embed("你好")).containsExactly(0.1f, 0.2f);
    }

    @Test
    void batchIsSlicedByBatchSize() {
        when(model.embed(org.mockito.ArgumentMatchers.<String>anyList())).thenAnswer(inv -> {
            List<String> in = inv.getArgument(0);
            List<float[]> out = new ArrayList<>();
            for (String s : in) {
                out.add(new float[]{s.length()});
            }
            return out;
        });

        List<String> input = List.of("a", "bb", "ccc", "dddd", "eeeee", "ffffff", "ggggggg");
        List<float[]> result = new EmbeddingModelAdapter(model, 3).embedBatch(input);

        assertThat(result).hasSize(7);
        assertThat(result.get(6)).containsExactly(7.0f);

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(model, times(3)).embed(captor.capture());
        assertThat(captor.getAllValues().get(0)).hasSize(3);  // 3 + 3 + 1
        assertThat(captor.getAllValues().get(2)).containsExactly("ggggggg");
    }

    @Test
    void emptyBatchReturnsEmptyWithoutCallingModel() {
        assertThat(new EmbeddingModelAdapter(model, 32).embedBatch(List.of())).isEmpty();
        verify(model, times(0)).embed(org.mockito.ArgumentMatchers.<String>anyList());
    }

    @Test
    void dimensionsDelegates() {
        when(model.dimensions()).thenReturn(384);

        assertThat(new EmbeddingModelAdapter(model, 32).dimensions()).isEqualTo(384);
    }
}
