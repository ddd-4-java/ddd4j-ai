package io.ddd4j.ai.extension.ocr.service.impl;

import io.ddd4j.ai.extension.ocr.service.OcrService;
import org.springframework.ai.document.Document;
import org.springframework.http.MediaType;

import java.io.InputStream;
import java.util.List;

/**
 * 策略组合实现：PDF 走 PDFBox 直提，其余格式走 Tika 多格式解析，
 * 端口层唯一 {@code OcrService} 入口，避免多实现注入歧义。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class CompositeOcrService implements OcrService {

    private final PdfBoxOcrService pdfBoxOcrService;
    private final TikaOcrService tikaOcrService;

    /**
     * 构造组合识别服务。
     *
     * @param pdfBoxOcrService PDF 直提实现
     * @param tikaOcrService   多格式回退实现
     */
    public CompositeOcrService(PdfBoxOcrService pdfBoxOcrService, TikaOcrService tikaOcrService) {
        this.pdfBoxOcrService = pdfBoxOcrService;
        this.tikaOcrService = tikaOcrService;
    }

    /**
     * 提取纯文本：PDF 媒体类型走 PDFBox，其余走 Tika。
     *
     * @param input     文档内容流
     * @param mediaType 媒体类型（null 视为非 PDF）
     * @return 提取的纯文本
     * @throws Exception 解析失败时原样抛出
     */
    @Override
    public String extractText(InputStream input, MediaType mediaType) throws Exception {
        if (pdfBoxOcrService != null && isPdf(mediaType)) {
            return pdfBoxOcrService.extractText(input, mediaType);
        }
        return tikaOcrService.extractText(input, mediaType);
    }

    /**
     * 提取为统一文档模型：PDF 媒体类型走 PDFBox，其余走 Tika。
     *
     * @param input     文档内容流
     * @param mediaType 媒体类型（null 视为非 PDF）
     * @return 文档模型列表
     * @throws Exception 解析失败时原样抛出
     */
    @Override
    public List<Document> extract(InputStream input, MediaType mediaType) throws Exception {
        if (pdfBoxOcrService != null && isPdf(mediaType)) {
            return pdfBoxOcrService.extract(input, mediaType);
        }
        return tikaOcrService.extract(input, mediaType);
    }

    private static boolean isPdf(MediaType mediaType) {
        return mediaType != null && MediaType.APPLICATION_PDF.equalsTypeAndSubtype(mediaType);
    }
}
