package io.ddd4j.ai.cmpt.document.parser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.tika.Tika;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.ocr.TesseractOCRParser;
import org.apache.tika.sax.TeeContentHandler;
import org.apache.tika.sax.ToMarkdownContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ContentHandler;

import io.ddd4j.ai.cmpt.asr.service.AsrService;
import io.ddd4j.ai.cmpt.asr.service.AudioFormat;
import io.ddd4j.ai.cmpt.document.Document;
import io.ddd4j.ai.cmpt.document.DocumentImage;
import io.ddd4j.ai.cmpt.document.DocumentParser;
import io.ddd4j.ai.cmpt.document.DocumentSection;
import io.ddd4j.ai.cmpt.document.DocumentTable;
import io.ddd4j.ai.cmpt.document.DocumentTooLargeException;
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
    private final AsrService asrService;

    public TikaDocumentParser() {
        this(new DocumentProperties(), null);
    }

    public TikaDocumentParser(DocumentProperties properties) {
        this(properties, null);
    }

    public TikaDocumentParser(DocumentProperties properties, AsrService asrService) {
        this.properties = properties;
        this.asrService = asrService;
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
        long limit = properties.getMaxFileSizeBytes();
        if (limit > 0 && Files.size(file.toPath()) > limit) {
            throw new DocumentTooLargeException(
                    "document exceeds size limit: " + file.getName() + " > " + limit + " bytes");
        }
        String mime = TIKA.detect(file.toPath());
        if (isAudio(mime) && asrService != null) {
            // 音频转写需完整字节（asr 端口契约），受同一大小上限保护
            byte[] bytes = Files.readAllBytes(file.toPath());
            return map(file.getName(), mime, appendTranscription(parse(file.toPath()), bytes));
        }
        return map(file.getName(), mime, parse(file.toPath()));
    }

    @Override
    public Document parse(InputStream in, String filename) throws Exception {
        Objects.requireNonNull(in, "in must not be null");
        File tmp = File.createTempFile("dai-doc-", "-" + (filename == null ? "tmp" : filename));
        try {
            copyLimited(in, tmp.toPath());
            return parse(tmp);
        } finally {
            tmp.delete();
        }
    }

    /** 限额复制：超过 max-file-size-bytes 抛 {@link DocumentTooLargeException}（流式计数，不全量入内存）。 */
    private void copyLimited(InputStream in, Path target) throws IOException {
        long limit = properties.getMaxFileSizeBytes();
        InputStream source = limit > 0 ? new LimitedInputStream(in, limit) : in;
        try (source; java.io.OutputStream out = Files.newOutputStream(target)) {
            source.transferTo(out);
        }
    }

    private static boolean isAudio(String mime) {
        return mime != null && mime.startsWith("audio/");
    }

    private Parsed appendTranscription(Parsed parsed, byte[] bytes) {
        try {
            String text = asrService.transcribe(bytes, AudioFormat.wav44100Stereo16());
            if (text == null || text.isBlank()) {
                return parsed;
            }
            List<DocumentSection> sections = new java.util.ArrayList<>(parsed.sections());
            sections.add(new DocumentSection("Transcription", 1, text.strip(),
                    java.util.List.of(), java.util.List.of(), java.util.List.of()));
            String markdown = parsed.markdown() == null || parsed.markdown().isBlank()
                    ? text
                    : parsed.markdown() + "\n\n## Transcription\n\n" + text;
            return new Parsed(markdown, sections, parsed.tables(), parsed.images(), parsed.tikaMetadata());
        } catch (Exception e) {
            return parsed; // 转写失败不中断（对齐 markitdown 的音频转写失败不阻塞哲学）
        }
    }

    private Parsed parse(Path path) throws Exception {
        EmbeddedImageExtractor extractor = new EmbeddedImageExtractor();
        try {
            return doParse(path, withTimeout(ocrEnabled() ? ocrContext(extractor) : context(extractor)), extractor);
        } catch (Exception e) {
            // OCR 引擎不可用（宿主机缺 tesseract）时降级：无 OCR 上下文重解析，
            // 保证智能体总能拿到结果（对齐 markitdown 的 OCR 失败不阻塞哲学）
            if (properties.isOcrEnabled() && !(e instanceof org.apache.tika.exception.TikaTimeoutException)) {
                return doParse(path, withTimeout(context(extractor)), extractor);
            }
            throw e;
        }
    }

    private boolean ocrEnabled() {
        return properties.isOcrEnabled();
    }

    private static ParseContext context(EmbeddedImageExtractor extractor) {
        ParseContext context = new ParseContext();
        context.set(EmbeddedDocumentExtractor.class, extractor);
        return context;
    }

    /** 注入解析超时：病态文档超时抛 TikaTimeoutException，不降级重试（重试只会再挂一次）。 */
    private ParseContext withTimeout(ParseContext context) {
        long timeout = properties.getParseTimeoutMillis();
        if (timeout > 0) {
            context.set(org.apache.tika.config.TikaTaskTimeout.class,
                    new org.apache.tika.config.TikaTaskTimeout(timeout));
        }
        return context;
    }

    private static ParseContext ocrContext(EmbeddedImageExtractor extractor) {
        ParseContext context = context(extractor);
        TesseractOCRConfig config = new TesseractOCRConfig();
        config.setOutputType(TesseractOCRConfig.OUTPUT_TYPE.TXT);
        context.set(TesseractOCRConfig.class, config);
        context.set(TesseractOCRParser.class, new TesseractOCRParser());
        return context;
    }

    /** 流式解析：TikaInputStream 按需 spool（大文件落盘临时文件），不整体入内存。 */
    private static Parsed doParse(Path path, ParseContext context, EmbeddedImageExtractor extractor) throws Exception {
        StringWriter writer = new StringWriter();
        MarkdownStructureHandler structure = new MarkdownStructureHandler();
        TeeContentHandler tee = new TeeContentHandler(new ToMarkdownContentHandler(writer), structure);
        try (org.apache.tika.io.TikaInputStream stream = org.apache.tika.io.TikaInputStream.get(path)) {
            Metadata metadata = new Metadata();
            new AutoDetectParser().parse(stream, tee, metadata, context);
            List<DocumentImage> images = new ArrayList<>(structure.images());
            images.addAll(extractor.images());
            return new Parsed(writer.toString(), structure.sections(), structure.tables(), images, metadata);
        }
    }

    /** 限额读流：累计字节超限即抛，避免全量入内存后才拒绝。 */
    private static final class LimitedInputStream extends FilterInputStream {

        private final long limit;
        private long read;

        private LimitedInputStream(InputStream in, long limit) {
            super(in);
            this.limit = limit;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = super.read(b, off, len);
            if (n > 0) {
                read += n;
                checkLimit();
            }
            return n;
        }

        @Override
        public int read() throws IOException {
            int c = super.read();
            if (c != -1) {
                read++;
                checkLimit();
            }
            return c;
        }

        private void checkLimit() {
            if (read > limit) {
                throw new DocumentTooLargeException("document exceeds size limit: > " + limit + " bytes");
            }
        }
    }

    /** 收集容器文档（docx/zip 等）内嵌图片 → base64 data URL（对齐 markitdown 的图片提取）。 */
    private static final class EmbeddedImageExtractor implements EmbeddedDocumentExtractor {

        private final List<DocumentImage> images = new ArrayList<>();

        List<DocumentImage> images() {
            return images;
        }

        @Override
        public boolean shouldParseEmbedded(Metadata metadata) {
            return true;
        }

        @Override
        public void parseEmbedded(InputStream stream, ContentHandler handler, Metadata metadata,
                                  boolean outputHtml) throws SAXException, IOException {
            byte[] data = stream.readAllBytes();
            String contentType = metadata.get(HttpHeaders.CONTENT_TYPE);
            if (contentType == null || !contentType.startsWith("image/")) {
                // POI/OOXML 路径可能不设 CONTENT_TYPE：按内容嗅探补判
                String detected = TIKA.detect(data, metadata.get("resourceName"));
                if (detected.startsWith("image/")) {
                    contentType = detected;
                }
            }
            if (contentType != null && contentType.startsWith("image/")) {
                String src = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(data);
                images.add(new DocumentImage(metadata.get("resourceName"), src));
            }
        }
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
