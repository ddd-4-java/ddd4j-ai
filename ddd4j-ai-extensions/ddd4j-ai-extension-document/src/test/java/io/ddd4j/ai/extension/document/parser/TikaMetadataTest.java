package io.ddd4j.ai.extension.document.parser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.ddd4j.ai.extension.document.Document;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TikaDocumentParser} 元数据透传与语言检测测试（对齐 markitdown 的元数据能力 + Tika 加分项）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class TikaMetadataTest {

    @Test
    void metadata_carriesTikaKeys(@TempDir Path tmp) throws Exception {
        File file = tmp.resolve("page.html").toFile();
        Files.writeString(file.toPath(), "<html><body><h1>Title</h1><p>Body</p></body></html>");
        Document document = new TikaDocumentParser().parse(file);
        assertThat(document.metadata()).containsEntry("source", "tika");
        assertThat(document.metadata().get("detectedMime")).isEqualTo("text/html");
    }

    @Test
    void languageDetectionEnabled_withoutDetector_fallsBackGracefully(@TempDir Path tmp) throws Exception {
        // Tika 3.3.2 core 无内置 LanguageDetector 实现（需可选 tika-langdetect-opennlp），
        // 无 detector 时解析不抛异常、metadata 不强制含 language
        File file = tmp.resolve("en.txt").toFile();
        Files.writeString(file.toPath(), "This is an English sentence used for language detection with enough sample characters.");
        Document document = new TikaDocumentParser().parse(file);
        assertThat(document.fullMarkdown()).contains("English");
        assertThat(document.metadata()).containsEntry("source", "tika");
    }

    @Test
    void languageDetectionDisabled_omitsLanguage(@TempDir Path tmp) throws Exception {
        File file = tmp.resolve("en.txt").toFile();
        Files.writeString(file.toPath(), "Plain English text for detection.");
        io.ddd4j.ai.extension.document.properties.DocumentProperties properties =
                new io.ddd4j.ai.extension.document.properties.DocumentProperties();
        properties.setEnableLanguageDetection(false);
        Document document = new TikaDocumentParser(properties).parse(file);
        assertThat(document.metadata()).doesNotContainKey("language");
    }
}
