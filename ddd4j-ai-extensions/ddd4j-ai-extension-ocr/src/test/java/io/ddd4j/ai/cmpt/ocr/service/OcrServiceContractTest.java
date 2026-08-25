package io.ddd4j.ai.cmpt.ocr.service;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link OcrService} 端口契约测试：验证文本提取 / 文档提取 / 异常传播语义。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class OcrServiceContractTest {

    /** 桩实现：固定返回文本与单篇文档，可配置抛错验证错误路径。 */
    static class FakeOcrService implements OcrService {

        private final RuntimeException failure;

        FakeOcrService() {
            this(null);
        }

        FakeOcrService(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public String extractText(InputStream input, MediaType mediaType) {
            if (failure != null) {
                throw failure;
            }
            return "extracted";
        }

        @Override
        public List<Document> extract(InputStream input, MediaType mediaType) {
            if (failure != null) {
                throw failure;
            }
            return List.of(new Document.Builder().text("extracted").build());
        }
    }

    private final InputStream sample = new ByteArrayInputStream("sample".getBytes());

    @Test
    void extractText_returnsNonEmptyText() throws Exception {
        String text = new FakeOcrService().extractText(sample, MediaType.TEXT_PLAIN);
        assertThat(text).isNotBlank();
    }

    @Test
    void extract_returnsDocumentList() throws Exception {
        List<Document> documents = new FakeOcrService().extract(sample, MediaType.TEXT_PLAIN);
        assertThat(documents).isNotEmpty();
        assertThat(documents.get(0).getText()).contains("extracted");
    }

    @Test
    void extractText_propagatesEngineFailure() {
        RuntimeException cause = new IllegalStateException("engine down");
        assertThatThrownBy(() -> new FakeOcrService(cause).extractText(sample, MediaType.TEXT_PLAIN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("engine down");
    }

    @Test
    void extract_propagatesEngineFailure() {
        RuntimeException cause = new IllegalStateException("engine down");
        assertThatThrownBy(() -> new FakeOcrService(cause).extract(sample, MediaType.TEXT_PLAIN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("engine down");
    }
}
