package io.ddd4j.ai.cmpt.document;

/**
 * 文档统一结构模型：智能体读取任意格式后的规范化输出。
 *
 * @param title        文档标题（converter 提取或文件名兜底）
 * @param mime         内容媒体类型
 * @param source       输出来源（markitdown4j / 4 组件委托 / Tika 兜底）—— 审计与智能体可信度判断
 * @param sections     结构化章节树
 * @param tables       文档级表格
 * @param images       文档级图片（src 为 base64 data URL 或 URL）
 * @param fullMarkdown 完整 Markdown 文本（智能体主消费形态）
 * @param metadata     附加元数据（页数、标题等）
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public record Document(
        String title,
        String mime,
        SourceType source,
        java.util.List<DocumentSection> sections,
        java.util.List<DocumentTable> tables,
        java.util.List<DocumentImage> images,
        String fullMarkdown,
        java.util.Map<String, Object> metadata) {

    public Document {
        sections = java.util.List.copyOf(sections);
        tables = java.util.List.copyOf(tables);
        images = java.util.List.copyOf(images);
        metadata = metadata == null ? java.util.Map.of() : java.util.Map.copyOf(metadata);
    }
}
