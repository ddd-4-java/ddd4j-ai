package io.ddd4j.ai.extension.document;

import java.beans.ConstructorProperties;

import java.util.Objects;

/**
 * 文档统一结构模型：智能体读取任意格式后的规范化输出。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */

public final class Document {

    private static final long serialVersionUID = 0L;

    private final String title;

    private final String mime;

    private final SourceType source;

    private final java.util.List<DocumentSection> sections;

    private final java.util.List<DocumentTable> tables;

    private final java.util.List<DocumentImage> images;

    private final String fullMarkdown;

    private final java.util.Map<String, Object> metadata;

    /**
 * @param title 文档标题（converter 提取或文件名兜底）
 * @param mime 内容媒体类型
 * @param source 输出来源（markitdown4j / 4 组件委托 / Tika 兜底）—— 审计与智能体可信度判断
 * @param sections 结构化章节树
 * @param tables 文档级表格
 * @param images 文档级图片（src 为 base64 data URL 或 URL）
 * @param fullMarkdown 完整 Markdown 文本（智能体主消费形态）
 * @param metadata 附加元数据（页数、标题等）
 */

    @ConstructorProperties({ "title", "mime", "source", "sections", "tables", "images", "fullMarkdown", "metadata" })
    public Document(String title, String mime, SourceType source, java.util.List<DocumentSection> sections, java.util.List<DocumentTable> tables, java.util.List<DocumentImage> images, String fullMarkdown, java.util.Map<String, Object> metadata) {
        sections = java.util.List.copyOf(sections);
        tables = java.util.List.copyOf(tables);
        images = java.util.List.copyOf(images);
        metadata = metadata == null ? java.util.Map.of() : java.util.Map.copyOf(metadata);
        this.title = title;
        this.mime = mime;
        this.source = source;
        this.sections = sections;
        this.tables = tables;
        this.images = images;
        this.fullMarkdown = fullMarkdown;
        this.metadata = metadata;
    }

    public String title() {
        return title;
    }

    public String mime() {
        return mime;
    }

    public SourceType source() {
        return source;
    }

    public java.util.List<DocumentSection> sections() {
        return sections;
    }

    public java.util.List<DocumentTable> tables() {
        return tables;
    }

    public java.util.List<DocumentImage> images() {
        return images;
    }

    public String fullMarkdown() {
        return fullMarkdown;
    }

    public java.util.Map<String, Object> metadata() {
        return metadata;
    }

    public String getTitle() {
        return title();
    }

    public String getMime() {
        return mime();
    }

    public SourceType getSource() {
        return source();
    }

    public java.util.List<DocumentSection> getSections() {
        return sections();
    }

    public java.util.List<DocumentTable> getTables() {
        return tables();
    }

    public java.util.List<DocumentImage> getImages() {
        return images();
    }

    public String getFullMarkdown() {
        return fullMarkdown();
    }

    public java.util.Map<String, Object> getMetadata() {
        return metadata();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (Objects.isNull(obj) || getClass() != obj.getClass()) {
            return false;
        }
        Document other = (Document) obj;
        return Objects.equals(this.title, other.title) && Objects.equals(this.mime, other.mime) && Objects.equals(this.source, other.source) && Objects.equals(this.sections, other.sections) && Objects.equals(this.tables, other.tables) && Objects.equals(this.images, other.images) && Objects.equals(this.fullMarkdown, other.fullMarkdown) && Objects.equals(this.metadata, other.metadata);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Objects.hashCode(title);
        result = 31 * result + Objects.hashCode(mime);
        result = 31 * result + Objects.hashCode(source);
        result = 31 * result + Objects.hashCode(sections);
        result = 31 * result + Objects.hashCode(tables);
        result = 31 * result + Objects.hashCode(images);
        result = 31 * result + Objects.hashCode(fullMarkdown);
        result = 31 * result + Objects.hashCode(metadata);
        return result;
    }

    @Override
    public String toString() {
        return "Document[title=" + title + ", mime=" + mime + ", source=" + source + ", sections=" + sections + ", tables=" + tables + ", images=" + images + ", fullMarkdown=" + fullMarkdown + ", metadata=" + metadata + "]";
    }
}
