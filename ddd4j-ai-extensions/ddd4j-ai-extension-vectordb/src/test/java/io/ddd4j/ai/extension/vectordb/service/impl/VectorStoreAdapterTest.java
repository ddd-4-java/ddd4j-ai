package io.ddd4j.ai.extension.vectordb.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link VectorStoreAdapter} 单元测试：mock VectorStore，验证委托与默认 topK。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class VectorStoreAdapterTest {

    private final VectorStore store = mock(VectorStore.class);
    private final VectorStoreAdapter adapter = new VectorStoreAdapter(store, 5);

    @Test
    void addAndDeleteDelegate() {
        List<Document> docs = List.of(new Document("x"));
        adapter.add(docs);
        verify(store).add(docs);

        adapter.delete(List.of("id1"));
        verify(store).delete(List.of("id1"));
    }

    @Test
    void searchUsesGivenTopKAndFilter() {
        adapter.search("query", 7, null);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(store).similaritySearch(captor.capture());
        assertThat(captor.getValue().getTopK()).isEqualTo(7);
    }

    @Test
    void searchFallsBackToDefaultTopKAndAppliesFilter() {
        Filter.Expression filter = new FilterExpressionBuilder().eq("k", "v").build();
        adapter.search("query", 0, filter);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(store).similaritySearch(captor.capture());
        assertThat(captor.getValue().getTopK()).isEqualTo(5);  // 默认值
        assertThat(captor.getValue().getFilterExpression()).isEqualTo(filter);
    }
}
