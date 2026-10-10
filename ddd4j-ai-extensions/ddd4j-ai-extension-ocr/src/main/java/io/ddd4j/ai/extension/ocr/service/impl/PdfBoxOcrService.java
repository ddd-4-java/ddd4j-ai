package io.ddd4j.ai.extension.ocr.service.impl;

import io.ddd4j.ai.extension.ocr.service.OcrService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.http.MediaType;

import java.io.InputStream;
import java.util.List;

/**
 * PDF 专用文本提取：基于 Apache PDFBox 直接抽取嵌入式文本层，
 * 不依赖 Tika 解析链，适合大批量 PDF 的高吞吐直提。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class PdfBoxOcrService implements OcrService {

    /**
     * 构造 PdfBoxOcrService 对象。
     */
    public PdfBoxOcrService() {
    }

    /**
     * PDFBox 直接抽取 PDF 文本层（全量读入内存后解析）。
     *
     * @param input     PDF 内容流
     * @param mediaType 媒体类型（本实现忽略，调用方保证为 PDF）
     * @return 抽取的纯文本
     * @throws Exception PDF 解析失败时原样抛出
     */
    @Override
    public String extractText(InputStream input, MediaType mediaType) throws Exception {
        try (PDDocument document = Loader.loadPDF(input.readAllBytes())) {
            return new PDFTextStripper().getText(document);
        }
    }

    /**
     * 将 PDF 文本包装为统一文档模型（单文档，附 sourceType/contentType 元数据）。
     *
     * @param input     PDF 内容流
     * @param mediaType 媒体类型（本实现忽略，调用方保证为 PDF）
     * @return 单元素文档模型列表
     * @throws Exception PDF 解析失败时原样抛出
     */
    @Override
    public List<Document> extract(InputStream input, MediaType mediaType) throws Exception {
        String text = extractText(input, mediaType);
        Document document = new Document.Builder()
                .text(text)
                .metadata("sourceType", "pdf")
                .metadata("contentType", MediaType.APPLICATION_PDF.toString())
                .build();
        return List.of(document);
    }
}
