package io.ddd4j.ai.cmpt.document.parser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;

import io.ddd4j.ai.cmpt.document.Document;
import io.ddd4j.ai.cmpt.document.DocumentParser;
import io.ddd4j.ai.cmpt.document.DocumentSection;
import io.ddd4j.ai.cmpt.document.MediaType;
import io.ddd4j.ai.cmpt.document.SourceType;

/**
 * Tika 通用解析适配器：AutoDetectParser 覆盖 PDF / Office / HTML / CSV / 纯文本等全格式，
 * 作为通用基础实现（最低优先级兜底）。MIME 类型由 Tika 内容嗅探 + 文件名联合检测
 * （{@link Tika#detect(byte[], String)}），不依赖手写后缀映射。<p>
 * 注：计划原定 markitdown4j 1.0.0 承担此角色，但其 class 文件为 Java 25 编译（version 69），
 * 项目 target Java 17 无法加载且无低版本可退，故按计划预留的 {@link SourceType#TIKA_FALLBACK}
 * 角色改用 Apache Tika 3.3.2（已在依赖治理中、Java 17 兼容）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public final class TikaDocumentParser implements DocumentParser {

    private static final Tika TIKA = new Tika();

    @Override
    public MediaType supports() {
        return MediaType.UNKNOWN;
    }

    @Override
    public int order() {
        return 0; // 兜底：让高质量委托 parser（order=10）优先
    }

    @Override
    public Document parse(File file) throws Exception {
        Objects.requireNonNull(file, "file must not be null");
        byte[] bytes = Files.readAllBytes(file.toPath());
        String mime = TIKA.detect(bytes, file.getName());
        return map(extractText(new ByteArrayInputStream(bytes)), file.getName(), mime);
    }

    @Override
    public Document parse(InputStream in, String filename) throws Exception {
        Objects.requireNonNull(in, "in must not be null");
        byte[] bytes = in.readAllBytes();
        String mime = TIKA.detect(bytes, filename);
        return map(extractText(new ByteArrayInputStream(bytes)), filename, mime);
    }

    private static String extractText(InputStream in) throws Exception {
        try (in) {
            BodyContentHandler handler = new BodyContentHandler(-1);
            new AutoDetectParser().parse(in, handler, new Metadata());
            return handler.toString();
        }
    }

    private static Document map(String text, String name, String mime) {
        String markdown = text == null ? "" : text.strip();
        List<DocumentSection> sections = new ArrayList<>();
        sections.add(new DocumentSection(name == null ? "Document" : name, 1, markdown,
                List.of(), List.of(), List.of()));
        return new Document(
                name == null ? "Document" : name,
                mime,
                SourceType.TIKA_FALLBACK,
                sections,
                List.of(),
                List.of(),
                markdown,
                Map.of("source", "tika", "detectedMime", mime));
    }
}
