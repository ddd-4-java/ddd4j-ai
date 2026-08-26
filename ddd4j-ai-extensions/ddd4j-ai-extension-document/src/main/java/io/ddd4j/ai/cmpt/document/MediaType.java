package io.ddd4j.ai.cmpt.document;

import java.util.Locale;

/**
 * 文档媒体类型：由文件名后缀判定，用于解析器路由。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public enum MediaType {

    PDF, DOCX, XLSX, PPTX, CSV, HTML, IPYNB, EPUB, RSS, WIKIPEDIA, ZIP, PLAINTEXT, MD, IMAGE, UNKNOWN;

    /**
     * 按文件名后缀推断媒体类型（大小写不敏感）；未知后缀返回 {@link #UNKNOWN}。
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
