package io.ddd4j.ai.extension.document;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 智能体文档读取统一门面：按媒体类型路由到解析器，委托未就位时自动降级到通用兜底。
 *
 * @author ddd4j
 */
public final class DocumentReader {

    private final List<DocumentParser> parsers;

    /**
     * 创建读取门面：解析器按 {@link DocumentParser#order()} 降序排列。
     *
     * @param parsers 可用的解析器列表
     */
    public DocumentReader(List<DocumentParser> parsers) {
        this.parsers = parsers.stream()
                .sorted(Comparator.comparingInt(DocumentParser::order).reversed())
                .toList();
    }

    private static String filenameSuffix(MediaType type) {
        return switch (type) {
            case PDF -> "f.pdf";
            case DOCX -> "f.docx";
            case XLSX -> "f.xlsx";
            case PPTX -> "f.pptx";
            case CSV -> "f.csv";
            case HTML -> "f.html";
            case IPYNB -> "f.ipynb";
            case EPUB -> "f.epub";
            case RSS -> "f.xml";
            case ZIP -> "f.zip";
            case PLAINTEXT -> "f.txt";
            case MD -> "f.md";
            default -> "f.bin";
        };
    }

    /**
     * 读取文件为统一文档模型。
     *
     * @param file 待读取的文件
     * @return 规范化后的文档模型
     * @throws Exception 解析失败；无可用解析器抛 {@link UnsupportedOperationException}
     */
    public Document read(File file) throws Exception {
        Objects.requireNonNull(file, "file must not be null");
        return read(file, MediaType.fromFilename(file.getName()));
    }

    /**
     * 读取输入流为统一文档模型（文件名用于媒体类型判定）。
     *
     * @param in       待读取的输入流
     * @param filename 原始文件名（用于媒体类型判定）
     * @return 规范化后的文档模型
     * @throws Exception 解析失败；无可用解析器抛 {@link UnsupportedOperationException}
     */
    public Document read(InputStream in, String filename) throws Exception {
        Objects.requireNonNull(in, "in must not be null");
        return read(in, MediaType.fromFilename(filename));
    }

    /**
     * 读取 URL 指向的文档。
     *
     * @param url 文档地址
     * @return 规范化后的文档模型
     * @throws Exception 打开流或解析失败
     */
    public Document read(URL url) throws Exception {
        Objects.requireNonNull(url, "url must not be null");
        try (InputStream in = url.openStream()) {
            return read(in, url.getPath());
        }
    }

    /**
     * 读取路径指向的文档。
     *
     * @param path 文件路径
     * @return 规范化后的文档模型
     * @throws Exception 解析失败；无可用解析器抛 {@link UnsupportedOperationException}
     */
    public Document read(Path path) throws Exception {
        Objects.requireNonNull(path, "path must not be null");
        return read(path.toFile());
    }

    private Document read(File file, MediaType type) throws Exception {
        for (DocumentParser parser : parsers) {
            if (parser.supports() != type && parser.supports() != MediaType.UNKNOWN) {
                continue;
            }
            try {
                return parser.parse(file);
            } catch (UnsupportedOperationException ex) {
                // 委托解析器未就位：尝试下一个（降级）
            }
        }
        throw new UnsupportedOperationException("No parser available for " + type);
    }

    private Document read(InputStream in, MediaType type) throws Exception {
        for (DocumentParser parser : parsers) {
            if (parser.supports() != type && parser.supports() != MediaType.UNKNOWN) {
                continue;
            }
            try {
                return parser.parse(in, filenameSuffix(type));
            } catch (UnsupportedOperationException ex) {
                // 降级
            }
        }
        throw new UnsupportedOperationException("No parser available for " + type);
    }
}
