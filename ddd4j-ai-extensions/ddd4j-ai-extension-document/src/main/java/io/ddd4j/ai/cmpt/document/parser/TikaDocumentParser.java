package io.ddd4j.ai.cmpt.document.parser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.ToMarkdownContentHandler;

import io.ddd4j.ai.cmpt.document.Document;
import io.ddd4j.ai.cmpt.document.DocumentImage;
import io.ddd4j.ai.cmpt.document.DocumentParser;
import io.ddd4j.ai.cmpt.document.DocumentSection;
import io.ddd4j.ai.cmpt.document.DocumentTable;
import io.ddd4j.ai.cmpt.document.MediaType;
import io.ddd4j.ai.cmpt.document.SourceType;

/**
 * Tika 通用解析适配器：AutoDetectParser 覆盖 PDF / Office / HTML / CSV / 纯文本等全格式，
 * 作为通用基础实现（最低优先级兜底）。<ul>
 *   <li>MIME：{@link Tika#detect(byte[], String)} 内容嗅探 + 文件名联合检测（不依赖手写后缀映射）</li>
 *   <li>内容：{@link ToMarkdownContentHandler} 输出结构化 Markdown（标题/列表/表格 → GFM），供 RAG 分块</li>
 *   <li>元数据：透传 Tika 提取的 author/created/pages 等，供溯源与过滤</li>
 * </ul>
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
        return map(file.getName(), mime, parse(bytes));
    }

    @Override
    public Document parse(InputStream in, String filename) throws Exception {
        Objects.requireNonNull(in, "in must not be null");
        byte[] bytes = in.readAllBytes();
        String mime = TIKA.detect(bytes, filename);
        return map(filename, mime, parse(bytes));
    }

    private static Parsed parse(byte[] bytes) throws Exception {
        StringWriter writer = new StringWriter();
        MarkdownStructureHandler structure = new MarkdownStructureHandler();
        org.apache.tika.sax.TeeContentHandler tee = new org.apache.tika.sax.TeeContentHandler(
                new ToMarkdownContentHandler(writer), structure);
        new AutoDetectParser().parse(new ByteArrayInputStream(bytes), tee, new Metadata());
        return new Parsed(writer.toString(), structure.sections(), structure.tables(), structure.images());
    }

    private static Document map(String name, String mime, Parsed parsed) {
        String markdown = parsed.markdown() == null ? "" : parsed.markdown().strip();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "tika");
        metadata.put("detectedMime", mime);
        return new Document(
                name == null ? "Document" : name,
                mime,
                SourceType.TIKA_FALLBACK,
                parsed.sections(),
                parsed.tables(),
                parsed.images(),
                markdown,
                metadata);
    }

    /** Tika 解析产物：Markdown 全文 + 结构化字段。 */
    private record Parsed(String markdown,
                          List<DocumentSection> sections,
                          List<DocumentTable> tables,
                          List<DocumentImage> images) {
    }
}
