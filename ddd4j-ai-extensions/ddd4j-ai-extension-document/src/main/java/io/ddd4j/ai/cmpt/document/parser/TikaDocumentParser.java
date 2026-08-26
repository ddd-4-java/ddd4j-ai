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
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.ocr.TesseractOCRParser;
import org.apache.tika.sax.TeeContentHandler;
import org.apache.tika.sax.ToMarkdownContentHandler;

import io.ddd4j.ai.cmpt.document.Document;
import io.ddd4j.ai.cmpt.document.DocumentImage;
import io.ddd4j.ai.cmpt.document.DocumentParser;
import io.ddd4j.ai.cmpt.document.DocumentSection;
import io.ddd4j.ai.cmpt.document.DocumentTable;
import io.ddd4j.ai.cmpt.document.MediaType;
import io.ddd4j.ai.cmpt.document.SourceType;
import io.ddd4j.ai.cmpt.document.properties.DocumentProperties;

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

    private final DocumentProperties properties;

    public TikaDocumentParser() {
        this(new DocumentProperties());
    }

    public TikaDocumentParser(DocumentProperties properties) {
        this.properties = properties;
    }

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

    private Parsed parse(byte[] bytes) throws Exception {
        try {
            return doParse(bytes, ocrEnabled() ? ocrContext() : null);
        } catch (Exception e) {
            // OCR 引擎不可用（宿主机缺 tesseract）时降级：无 OCR 上下文重解析，
            // 保证智能体总能拿到结果（对齐 markitdown 的 OCR 失败不阻塞哲学）
            if (properties.isOcrEnabled()) {
                return doParse(bytes, null);
            }
            throw e;
        }
    }

    private boolean ocrEnabled() {
        return properties.isOcrEnabled();
    }

    private static ParseContext ocrContext() {
        ParseContext context = new ParseContext();
        TesseractOCRConfig config = new TesseractOCRConfig();
        config.setOutputType(TesseractOCRConfig.OUTPUT_TYPE.TXT);
        context.set(TesseractOCRConfig.class, config);
        context.set(TesseractOCRParser.class, new TesseractOCRParser());
        return context;
    }

    private static Parsed doParse(byte[] bytes, ParseContext context) throws Exception {
        StringWriter writer = new StringWriter();
        MarkdownStructureHandler structure = new MarkdownStructureHandler();
        TeeContentHandler tee = new TeeContentHandler(new ToMarkdownContentHandler(writer), structure);
        ParseContext effective = context == null ? new ParseContext() : context;
        Metadata metadata = new Metadata();
        new AutoDetectParser().parse(new ByteArrayInputStream(bytes), tee, metadata, effective);
        return new Parsed(writer.toString(), structure.sections(), structure.tables(), structure.images(), metadata);
    }

    private Document map(String name, String mime, Parsed parsed) {
        String markdown = parsed.markdown() == null ? "" : parsed.markdown().strip();
        Map<String, Object> metadata = new HashMap<>();
        // 全量透传 Tika 元数据（EXIF / 音频 / 办公作者时间页数等，markitdown 无此能力）
        for (String key : parsed.tikaMetadata().names()) {
            metadata.put(key, parsed.tikaMetadata().get(key));
        }
        // 规范化精选键（对 RAG 溯源友好）
        putIfAbsent(metadata, "author", first(parsed.tikaMetadata(), "dc:creator", "Author"));
        putIfAbsent(metadata, "created", first(parsed.tikaMetadata(), "dcterms:created", "Creation-Date"));
        putIfAbsent(metadata, "pageCount", first(parsed.tikaMetadata(), "xmpTPg:NPages"));
        if (properties.isEnableLanguageDetection()) {
            putIfAbsent(metadata, "language", detectLanguage(markdown));
        }
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

    private static void putIfAbsent(Map<String, Object> target, String key, Object value) {
        if (value != null && !String.valueOf(value).isBlank() && !target.containsKey(key)) {
            target.put(key, value);
        }
    }

    private static String first(Metadata metadata, String... keys) {
        for (String key : keys) {
            String value = metadata.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String detectLanguage(String text) {
        try {
            if (text == null || text.isBlank()) {
                return null;
            }
            org.apache.tika.language.detect.LanguageResult result =
                    org.apache.tika.language.detect.LanguageDetector.getDefaultLanguageDetector()
                            .detect(text.length() > 2000 ? text.substring(0, 2000) : text);
            String language = result.getLanguage();
            return language == null || language.isBlank() || "unknown".equals(language) ? null : language;
        } catch (Exception e) {
            return null; // 语言检测不可用（无模型）不阻塞解析
        }
    }

    /** Tika 解析产物：Markdown 全文 + 结构化字段 + 原始元数据。 */
    private record Parsed(String markdown,
                          List<DocumentSection> sections,
                          List<DocumentTable> tables,
                          List<DocumentImage> images,
                          Metadata tikaMetadata) {
    }
}
