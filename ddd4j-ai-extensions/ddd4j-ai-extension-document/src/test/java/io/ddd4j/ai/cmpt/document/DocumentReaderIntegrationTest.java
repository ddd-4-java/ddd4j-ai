package io.ddd4j.ai.cmpt.document;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.ddd4j.ai.cmpt.document.autoconfigure.DocumentAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DocumentReader} 集成测试：真实 Spring 上下文端到端（纯文本 / 真实 PDF / 委托降级）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@SpringBootTest(classes = DocumentAutoConfiguration.class)
class DocumentReaderIntegrationTest {

    @Autowired
    private DocumentReader reader;

    @Test
    void readerHandlesPlainTextWithTika(@TempDir Path tmp) throws Exception {
        File file = tmp.resolve("hello.txt").toFile();
        Files.writeString(file.toPath(), "Hello\n\n## Sub\n\nWorld");
        Document document = reader.read(file);
        assertThat(document.source()).isEqualTo(SourceType.TIKA_FALLBACK);
        // Tika markdown 输出会把纯文本中的 # 转义为 \#（markdown 合规），内容保留
        assertThat(document.fullMarkdown()).contains("Hello").contains("Sub").contains("World");
    }

    @Test
    void readerHandlesRealPdfWithDelegationFallback(@TempDir Path tmp) throws Exception {
        File pdf = tmp.resolve("sample.pdf").toFile();
        createPdf(pdf, "Invoice 2026\nTotal: 100");

        Document document = reader.read(pdf);

        // 默认装配：EasypdfDocumentParser(order=10) 未就位抛 UnsupportedOperationException → 降级 Tika
        assertThat(document.source()).isEqualTo(SourceType.TIKA_FALLBACK);
        assertThat(document.fullMarkdown()).contains("Invoice 2026");
        assertThat(document.fullMarkdown()).contains("Total: 100");
    }

    @Test
    void readerHandlesInputStream(@TempDir Path tmp) throws Exception {
        File file = tmp.resolve("note.md").toFile();
        Files.writeString(file.toPath(), "# Title\n\nBody text");
        try (java.io.InputStream in = Files.newInputStream(file.toPath())) {
            Document document = reader.read(in, "note.md");
            assertThat(document.source()).isEqualTo(SourceType.TIKA_FALLBACK);
            assertThat(document.fullMarkdown()).contains("Title").contains("Body text");
        }
    }

    private static void createPdf(File target, String content) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(60, 720);
                for (String line : content.split("\n")) {
                    stream.showText(line);
                    stream.newLineAtOffset(0, -16);
                }
                stream.endText();
            }
            document.save(target);
        }
    }
}
