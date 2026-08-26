package io.ddd4j.ai.cmpt.document.parser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.ddd4j.ai.cmpt.document.Document;
import io.ddd4j.ai.cmpt.document.DocumentTooLargeException;
import io.ddd4j.ai.cmpt.document.SourceType;
import io.ddd4j.ai.cmpt.document.properties.DocumentProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link TikaDocumentParser} 文件大小上限测试：超限拒绝（File/Stream 两入口）+ 不限制回归。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class SizeLimitTest {

    private static DocumentProperties limit(long maxBytes) {
        DocumentProperties properties = new DocumentProperties();
        properties.setMaxFileSizeBytes(maxBytes);
        return properties;
    }

    @Test
    void fileExceedsLimit_throwsDocumentTooLarge(@TempDir Path tmp) throws Exception {
        File file = tmp.resolve("big.txt").toFile();
        Files.writeString(file.toPath(), "12345678901"); // 11 bytes
        TikaDocumentParser parser = new TikaDocumentParser(limit(10));
        assertThatThrownBy(() -> parser.parse(file))
                .isInstanceOf(DocumentTooLargeException.class)
                .hasMessageContaining("10");
    }

    @Test
    void streamExceedsLimit_throwsDocumentTooLarge() {
        TikaDocumentParser parser = new TikaDocumentParser(limit(10));
        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(new byte[20]), "big.txt"))
                .isInstanceOf(DocumentTooLargeException.class);
    }

    @Test
    void limitDisabled_acceptsAnySize(@TempDir Path tmp) throws Exception {
        File file = tmp.resolve("note.txt").toFile();
        Files.writeString(file.toPath(), "any size content");
        Document document = new TikaDocumentParser(limit(0)).parse(file);
        assertThat(document.fullMarkdown()).contains("any size content");
    }

    @Test
    void normalSizedFile_unchangedBehavior(@TempDir Path tmp) throws Exception {
        File file = tmp.resolve("note.txt").toFile();
        Files.writeString(file.toPath(), "hello limit");
        Document document = new TikaDocumentParser().parse(file);
        assertThat(document.source()).isEqualTo(SourceType.TIKA_FALLBACK);
        assertThat(document.fullMarkdown()).contains("hello limit");
    }

    @Test
    void boundedStream_closesUnderlying(@TempDir Path tmp) throws Exception {
        // 限额包装流关闭底层流（无泄漏）
        java.util.concurrent.atomic.AtomicBoolean closed = new java.util.concurrent.atomic.AtomicBoolean();
        InputStream in = new FilterInputStream(new ByteArrayInputStream("tiny".getBytes())) {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        };
        new TikaDocumentParser(limit(0)).parse(in, "a.txt");
        assertThat(closed).isTrue();
    }
}
