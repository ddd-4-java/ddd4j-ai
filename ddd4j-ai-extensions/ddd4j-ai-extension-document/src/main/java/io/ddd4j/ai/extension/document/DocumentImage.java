package io.ddd4j.ai.extension.document;

/**
 * 文档图片：替代文本 + 内容地址（base64 data URL 或 URL）。
 *
 * @param alt 替代文本
 * @param src 图片地址
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public record DocumentImage(String alt, String src) {
}
