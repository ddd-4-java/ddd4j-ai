package io.ddd4j.ai.extension.document;

import java.beans.ConstructorProperties;

import java.util.Objects;

import java.util.List;

/**
 * 文档章节：支持层级嵌套（children）与内联表格/图片。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */

public final class DocumentSection {

    private static final long serialVersionUID = 0L;

    private final String title;

    private final int level;

    private final String content;

    private final List<DocumentSection> children;

    private final List<DocumentTable> tables;

    private final List<DocumentImage> images;

    /**
 * 规范构造器：子章节/表格/图片固化为不可变列表。
 *
 * @param title 章节标题
 * @param level 层级深度（1 起）
 * @param content 章节正文（Markdown 文本）
 * @param children 子章节
 * @param tables 章节内表格
 * @param images 章节内图片
 */

    @ConstructorProperties({ "title", "level", "content", "children", "tables", "images" })
    public DocumentSection(String title, int level, String content, List<DocumentSection> children, List<DocumentTable> tables, List<DocumentImage> images) {
        children = List.copyOf(children);
        tables = List.copyOf(tables);
        images = List.copyOf(images);
        this.title = title;
        this.level = level;
        this.content = content;
        this.children = children;
        this.tables = tables;
        this.images = images;
    }

    public String title() {
        return title;
    }

    public int level() {
        return level;
    }

    public String content() {
        return content;
    }

    public List<DocumentSection> children() {
        return children;
    }

    public List<DocumentTable> tables() {
        return tables;
    }

    public List<DocumentImage> images() {
        return images;
    }

    public String getTitle() {
        return title();
    }

    public int getLevel() {
        return level();
    }

    public String getContent() {
        return content();
    }

    public List<DocumentSection> getChildren() {
        return children();
    }

    public List<DocumentTable> getTables() {
        return tables();
    }

    public List<DocumentImage> getImages() {
        return images();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (Objects.isNull(obj) || getClass() != obj.getClass()) {
            return false;
        }
        DocumentSection other = (DocumentSection) obj;
        return Objects.equals(this.title, other.title) && this.level == other.level && Objects.equals(this.content, other.content) && Objects.equals(this.children, other.children) && Objects.equals(this.tables, other.tables) && Objects.equals(this.images, other.images);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Objects.hashCode(title);
        result = 31 * result + Integer.hashCode(level);
        result = 31 * result + Objects.hashCode(content);
        result = 31 * result + Objects.hashCode(children);
        result = 31 * result + Objects.hashCode(tables);
        result = 31 * result + Objects.hashCode(images);
        return result;
    }

    @Override
    public String toString() {
        return "DocumentSection[title=" + title + ", level=" + level + ", content=" + content + ", children=" + children + ", tables=" + tables + ", images=" + images + "]";
    }
}
