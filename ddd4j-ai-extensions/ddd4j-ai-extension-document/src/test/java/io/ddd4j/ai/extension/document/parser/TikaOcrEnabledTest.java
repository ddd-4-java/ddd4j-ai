package io.ddd4j.ai.extension.document.parser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.ddd4j.ai.extension.document.Document;
import io.ddd4j.ai.extension.document.SourceType;
import io.ddd4j.ai.extension.document.properties.DocumentProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TikaDocumentParser} OCR 开关测试：默认关闭 / 开启无 tesseract 优雅降级 / 图像元数据解析。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class TikaOcrEnabledTest {

    /** 1x1 透明 PNG（base64）。 */
    private static final byte[] ONE_PX_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

    @Test
    void ocrEnabled_false_default(@TempDir Path tmp) throws Exception {
        File file = tmp.resolve("note.txt").toFile();
        Files.writeString(file.toPath(), "plain text");
        Document document = new TikaDocumentParser(new DocumentProperties()).parse(file);
        assertThat(document.fullMarkdown()).contains("plain text");
        assertThat(document.source()).isEqualTo(SourceType.TIKA_FALLBACK);
    }

    @Test
    void ocrEnabled_true_withoutTesseract_fallsBackToPlainText(@TempDir Path tmp) throws Exception {
        DocumentProperties properties = new DocumentProperties();
        properties.setOcrEnabled(true);
        File file = tmp.resolve("note.txt").toFile();
        Files.writeString(file.toPath(), "hello without ocr");

        Document document = new TikaDocumentParser(properties).parse(file);

        // 宿主机无 tesseract 时 OCR 尝试失败 → 降级无 OCR 重解析，内容不丢
        assertThat(document.source()).isEqualTo(SourceType.TIKA_FALLBACK);
        assertThat(document.fullMarkdown()).contains("hello without ocr");
    }

    @Test
    void ocrEnabled_true_imageParsesMetadata(@TempDir Path tmp) throws Exception {
        DocumentProperties properties = new DocumentProperties();
        properties.setOcrEnabled(true);
        File file = tmp.resolve("pixel.png").toFile();
        Files.write(file.toPath(), ONE_PX_PNG);

        Document document = new TikaDocumentParser(properties).parse(file);

        // 无论是否降级，图像本身可解析（ImageParser 元数据路径），不抛异常
        assertThat(document.source()).isEqualTo(SourceType.TIKA_FALLBACK);
        assertThat(document.mime()).startsWith("image/");
    }
}
