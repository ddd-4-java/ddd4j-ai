package io.ddd4j.ai.cmpt.document.parser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.ddd4j.ai.cmpt.document.Document;
import io.ddd4j.ai.cmpt.document.MediaType;
import io.ddd4j.ai.cmpt.document.SourceType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link TikaDocumentParser} 单元测试：兜底语义 / 空参校验 / 纯文本与 HTML 端到端解析。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class TikaDocumentParserTest {

    @Test
    void adapterSupportsAllStandardTypes() {
        TikaDocumentParser parser = new TikaDocumentParser();
        assertThat(parser.order()).isZero();
        assertThat(parser.supports()).isEqualTo(MediaType.UNKNOWN); // 兜底：匹配所有类型
    }

    @Test
    void parseRejectsNullFile() {
        assertThatThrownBy(() -> new TikaDocumentParser().parse((File) null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void parseSimpleTextFile(@TempDir Path tmp) throws Exception {
        File file = tmp.resolve("hello.txt").toFile();
        Files.writeString(file.toPath(), "Hello World\nLine 2");
        Document document = new TikaDocumentParser().parse(file);
        assertThat(document.source()).isEqualTo(SourceType.TIKA_FALLBACK);
        assertThat(document.fullMarkdown()).contains("Hello World").contains("Line 2");
        assertThat(document.mime()).isEqualTo("text/plain"); // Tika 检测（内容+文件名）
    }

    @Test
    void parseHtmlFile(@TempDir Path tmp) throws Exception {
        File file = tmp.resolve("page.html").toFile();
        Files.writeString(file.toPath(), "<html><body><h1>Title</h1><p>Body text</p></body></html>");
        Document document = new TikaDocumentParser().parse(file);
        assertThat(document.source()).isEqualTo(SourceType.TIKA_FALLBACK);
        assertThat(document.fullMarkdown()).contains("Title").contains("Body text");
        assertThat(document.fullMarkdown()).contains("# Title"); // Tika 将 h1 转为结构化 markdown 标题
        assertThat(document.mime()).isEqualTo("text/html");
        assertThat(document.metadata()).containsEntry("source", "tika");
    }

    @Test
    void parseFileWithoutExtension_detectsByContent(@TempDir Path tmp) throws Exception {
        File file = tmp.resolve("unknown").toFile();
        Files.writeString(file.toPath(), "<html><body><p>sniffed</p></body></html>");
        Document document = new TikaDocumentParser().parse(file);
        // 无扩展名：Tika 按内容嗅探识别为 HTML
        assertThat(document.mime()).isEqualTo("text/html");
    }

    @Test
    void parseInputStreamWithFilename(@TempDir Path tmp) throws Exception {
        File file = tmp.resolve("note.txt").toFile();
        Files.writeString(file.toPath(), "Note body");
        Document document = new TikaDocumentParser().parse(Files.newInputStream(file.toPath()), "note.txt");
        assertThat(document.source()).isEqualTo(SourceType.TIKA_FALLBACK);
        assertThat(document.fullMarkdown()).contains("Note body");
    }
}
