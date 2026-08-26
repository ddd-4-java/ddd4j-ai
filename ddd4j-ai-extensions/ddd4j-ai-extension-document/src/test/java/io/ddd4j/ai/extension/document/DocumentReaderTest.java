package io.ddd4j.ai.extension.document;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link DocumentReader} 门面单元测试：优先级路由 / 未就位降级 / 空参校验。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class DocumentReaderTest {

    private final TikaDocumentParserStub tika = new TikaDocumentParserStub();
    private final DocumentReader reader = new DocumentReader(List.of(tika));

    @Test
    void readerRejectsNullFile() {
        assertThatThrownBy(() -> reader.read((File) null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void readerRejectsNullPath() {
        assertThatThrownBy(() -> reader.read((Path) null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void readerFallsBackToLowerPriorityParserWhenHighPriorityPending(@TempDir Path tmp) throws Exception {
        File file = tmp.resolve("a.pdf").toFile();
        Files.writeString(file.toPath(), "pdf fallback content");
        // 高优先级委托未就位（抛 UnsupportedOperationException）→ 降级到 Tika 兜底
        DocumentReader r = new DocumentReader(List.of(
                new PendingPdfParser(),
                new TikaDocumentParserStub()));
        Document document = r.read(file);
        assertThat(document.source()).isEqualTo(SourceType.TIKA_FALLBACK);
        assertThat(document.fullMarkdown()).contains("pdf fallback content");
    }

    @Test
    void readerThrowsWhenNoParserMatches(@TempDir Path tmp) throws Exception {
        File file = tmp.resolve("a.pptx").toFile();
        Files.writeString(file.toPath(), "ppt");
        DocumentReader r = new DocumentReader(List.of(new PendingPdfParser()));
        assertThatThrownBy(() -> r.read(file)).isInstanceOf(UnsupportedOperationException.class);
    }

    /** 高优先级委托未就位桩。 */
    static class PendingPdfParser implements DocumentParser {

        @Override
        public MediaType supports() {
            return MediaType.PDF;
        }

        @Override
        public int order() {
            return 10;
        }

        @Override
        public Document parse(File file) {
            throw new UnsupportedOperationException("easypdf pending");
        }

        @Override
        public Document parse(java.io.InputStream in, String filename) {
            throw new UnsupportedOperationException("easypdf pending");
        }
    }

    /** 通用兜底桩（UNKNOWN 匹配任意类型）。 */
    static class TikaDocumentParserStub implements DocumentParser {

        @Override
        public MediaType supports() {
            return MediaType.UNKNOWN;
        }

        @Override
        public int order() {
            return 0;
        }

        @Override
        public Document parse(File file) throws Exception {
            return new Document(file.getName(), "text/plain", SourceType.TIKA_FALLBACK,
                    List.of(new DocumentSection(file.getName(), 1, Files.readString(file.toPath()),
                            List.of(), List.of(), List.of())),
                    List.of(), List.of(), Files.readString(file.toPath()), java.util.Map.of());
        }

        @Override
        public Document parse(java.io.InputStream in, String filename) throws Exception {
            return parse(File.createTempFile("stub-", filename == null ? ".txt" : filename));
        }
    }
}
