package io.ddd4j.ai.extension.document;

/**
 * 文档解析输出来源：追踪每次读取由哪个引擎产出（智能体可信度与审计）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public enum SourceType {

    /** markitdown4j 全 MIT 转换套件（通用基础实现）。 */
    MARKITDOWN4J,

    /** easy4j PDF 组件（1:1 结构还原）。 */
    EASYPDF,

    /** easy4j DOC 组件（1:1 结构还原）。 */
    EASYDOC,

    /** easy4j EXCEL 组件（1:1 结构还原）。 */
    EASYEXCEL,

    /** easy4j ODF 组件（1:1 结构还原）。 */
    EASYODF,

    /** Apache Tika 兜底解析。 */
    TIKA_FALLBACK
}
