package io.ddd4j.ai.extension.ocr.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TikaOcrService} 单元测试：纯文本 / HTML 多格式提取（无需 Docker）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class TikaOcrServiceTest {

    @Test
    void extractText_plainText() throws Exception {
        byte[] bytes = "hello tika world".getBytes(StandardCharsets.UTF_8);
        String text = new TikaOcrService(false)
                .extractText(new ByteArrayInputStream(bytes), MediaType.TEXT_PLAIN);
        assertThat(text).contains("hello tika world");
    }

    @Test
    void extractText_html() throws Exception {
        byte[] bytes = ("<html><head><title>Doc</title></head>"
                + "<body><h1>Heading</h1><p>paragraph text</p></body></html>")
                .getBytes(StandardCharsets.UTF_8);
        String text = new TikaOcrService(false)
                .extractText(new ByteArrayInputStream(bytes), MediaType.TEXT_HTML);
        assertThat(text).contains("Heading").contains("paragraph text");
    }

    @Test
    void extract_returnsDocumentWithMetadata() throws Exception {
        byte[] bytes = "content".getBytes(StandardCharsets.UTF_8);
        var documents = new TikaOcrService(false)
                .extract(new ByteArrayInputStream(bytes), MediaType.TEXT_PLAIN);
        assertThat(documents).hasSize(1);
        assertThat(documents.get(0).getText()).contains("content");
        assertThat(documents.get(0).getMetadata()).containsEntry("sourceType", "tika");
    }
}
