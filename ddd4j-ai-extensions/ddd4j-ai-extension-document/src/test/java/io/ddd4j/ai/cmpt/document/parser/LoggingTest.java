package io.ddd4j.ai.cmpt.document.parser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import io.ddd4j.ai.cmpt.document.Document;
import io.ddd4j.ai.cmpt.document.properties.DocumentProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TikaDocumentParser} 可观测性测试：成功解析 INFO 指标 / 降级与拒绝 WARN 事件。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class LoggingTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(TikaDocumentParser.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    private List<ILoggingEvent> events(Level level) {
        return appender.list.stream().filter(e -> e.getLevel() == level).toList();
    }

    @Test
    void successfulParse_logsInfoWithMetrics(@TempDir Path tmp) throws Exception {
        File file = tmp.resolve("note.txt").toFile();
        Files.writeString(file.toPath(), "hello log");
        Document document = new TikaDocumentParser().parse(file);

        assertThat(document.fullMarkdown()).contains("hello log");
        List<ILoggingEvent> infos = events(Level.INFO);
        assertThat(infos).anyMatch(e -> e.getFormattedMessage().contains("document parsed")
                && e.getFormattedMessage().contains("text/plain")
                && e.getFormattedMessage().contains("tookMs="));
    }

    @Test
    void sizeRejection_logsWarn(@TempDir Path tmp) throws Exception {
        DocumentProperties properties = new DocumentProperties();
        properties.setMaxFileSizeBytes(5);
        File file = tmp.resolve("big.txt").toFile();
        Files.writeString(file.toPath(), "123456789");

        try {
            new TikaDocumentParser(properties).parse(file);
        } catch (IllegalArgumentException expected) {
            // 预期拒绝
        }

        assertThat(events(Level.WARN)).anyMatch(e -> e.getFormattedMessage().contains("exceeds size limit"));
    }

    @Test
    void truncation_logsWarn(@TempDir Path tmp) throws Exception {
        DocumentProperties properties = new DocumentProperties();
        properties.setMaxEmbeddedImageBytes(10); // 任何 PNG 必超限 → 截断
        File file = tmp.resolve("img.docx").toFile();
        Files.write(file.toPath(), docxWithOneImage());

        Document document = new TikaDocumentParser(properties).parse(file);

        assertThat(document.metadata()).containsEntry("embeddedImagesTruncated", true);
        assertThat(events(Level.WARN)).anyMatch(e -> e.getFormattedMessage().contains("truncated"));
    }

    private static byte[] docxWithOneImage() throws Exception {
        byte[] png = java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(out)) {
            zip.putNextEntry(new java.util.zip.ZipEntry("[Content_Types].xml"));
            zip.write(("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                      <Default Extension="xml" ContentType="application/xml"/>
                      <Default Extension="png" ContentType="image/png"/>
                      <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                    </Types>
                    """).getBytes());
            zip.closeEntry();
            zip.putNextEntry(new java.util.zip.ZipEntry("_rels/.rels"));
            zip.write(("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId0" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                    </Relationships>
                    """).getBytes());
            zip.closeEntry();
            zip.putNextEntry(new java.util.zip.ZipEntry("word/document.xml"));
            zip.write(("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                                xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
                                xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
                                xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
                                xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
                      <w:body><w:p><w:r><w:t>img doc</w:t></w:r></w:p>
                        <w:p><w:r><w:drawing><wp:inline><a:graphic><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:pic><pic:blipFill><a:blip r:embed="rId1"/></pic:blipFill></pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>
                      </w:body>
                    </w:document>
                    """).getBytes());
            zip.closeEntry();
            zip.putNextEntry(new java.util.zip.ZipEntry("word/_rels/document.xml.rels"));
            zip.write(("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/image1.png"/>
                    </Relationships>
                    """).getBytes());
            zip.closeEntry();
            zip.putNextEntry(new java.util.zip.ZipEntry("word/media/image1.png"));
            zip.write(png);
            zip.closeEntry();
        }
        return out.toByteArray();
    }
}
