package io.ddd4j.ai.extension.document;

import java.util.List;

/**
 * 文档章节：支持层级嵌套（children）与内联表格/图片。
 *
 * @param title   章节标题
 * @param level   层级深度（1 起）
 * @param content 章节正文（Markdown 文本）
 * @param children 子章节
 * @param tables  章节内表格
 * @param images  章节内图片
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public record DocumentSection(
        String title,
        int level,
        String content,
        List<DocumentSection> children,
        List<DocumentTable> tables,
        List<DocumentImage> images) {

    public DocumentSection {
        children = List.copyOf(children);
        tables = List.copyOf(tables);
        images = List.copyOf(images);
    }
}
