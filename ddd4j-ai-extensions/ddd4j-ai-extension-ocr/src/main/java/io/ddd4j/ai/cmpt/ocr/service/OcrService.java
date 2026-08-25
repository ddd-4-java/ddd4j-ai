package io.ddd4j.ai.cmpt.ocr.service;

import org.springframework.ai.document.Document;
import org.springframework.http.MediaType;

import java.io.InputStream;
import java.util.List;

/**
 * 文档/图像识别端口：从字节流按媒体类型提取文本或结构化文档。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public interface OcrService {

    /**
     * 提取纯文本：PDF 嵌入式文本 / Office / HTML / 图像（OCR 按实现能力）。
     *
     * @param input     文档字节流（调用方负责关闭）
     * @param mediaType 内容媒体类型；未知可传 {@code null} 由实现自动探测
     * @return 提取到的全部文本
     * @throws Exception 解析失败（格式损坏 / 引擎不可用）
     */
    String extractText(InputStream input, MediaType mediaType) throws Exception;

    /**
     * 提取结构化文档列表：每篇文档带文本与来源元数据，可直接投喂向量库/RAG。
     *
     * @param input     文档字节流（调用方负责关闭）
     * @param mediaType 内容媒体类型；未知可传 {@code null} 由实现自动探测
     * @return 文档列表（至少一篇；空内容返回含空文本的单篇）
     * @throws Exception 解析失败（格式损坏 / 引擎不可用）
     */
    List<Document> extract(InputStream input, MediaType mediaType) throws Exception;
}
