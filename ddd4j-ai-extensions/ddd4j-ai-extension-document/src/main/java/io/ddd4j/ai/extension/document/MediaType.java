package io.ddd4j.ai.extension.document;

import java.util.Locale;

/**
 * 文档媒体类型：由文件名后缀判定，用于解析器路由。
 *
 * @author ddd4j
 */
public enum MediaType {

    /** 便携文档格式。 */
    PDF,
    /** Word 文档。 */
    DOCX,
    /** Excel 表格。 */
    XLSX,
    /** PowerPoint 演示文稿。 */
    PPTX,
    /** 逗号分隔值。 */
    CSV,
    /** HTML 页面。 */
    HTML,
    /** Jupyter Notebook。 */
    IPYNB,
    /** 电子书。 */
    EPUB,
    /** RSS/XML 订阅源。 */
    RSS,
    /** 维基百科页面导出。 */
    WIKIPEDIA,
    /** ZIP 压缩包。 */
    ZIP,
    /** 纯文本。 */
    PLAINTEXT,
    /** Markdown 文档。 */
    MD,
    /** 图片文件。 */
    IMAGE,
    /** 未知类型（通用兜底）。 */
    UNKNOWN;

    /**
     * 按文件名后缀推断媒体类型（大小写不敏感）；未知后缀返回 {@link #UNKNOWN}。
     *
     * @param filename 文件名，可为 {@code null}
     * @return 推断出的媒体类型，无法识别时为 {@link #UNKNOWN}
     */
    public static MediaType fromFilename(String filename) {
        if (filename == null) {
            return UNKNOWN;
        }
        String name = filename.toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        String ext = dot >= 0 ? name.substring(dot + 1) : name;
        return switch (ext) {
            case "pdf" -> PDF;
            case "docx" -> DOCX;
            case "xlsx" -> XLSX;
            case "pptx" -> PPTX;
            case "csv" -> CSV;
            case "html", "htm" -> HTML;
            case "ipynb" -> IPYNB;
            case "epub" -> EPUB;
            case "rss", "xml" -> RSS;
            case "zip" -> ZIP;
            case "txt" -> PLAINTEXT;
            case "md", "markdown" -> MD;
            case "png", "jpg", "jpeg", "gif", "bmp", "webp", "svg" -> IMAGE;
            default -> UNKNOWN;
        };
    }
}
