package io.ddd4j.ai.cmpt.ocr.service.impl;

import io.ddd4j.ai.cmpt.ocr.service.OcrService;
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

    public CompositeOcrService(PdfBoxOcrService pdfBoxOcrService, TikaOcrService tikaOcrService) {
        this.pdfBoxOcrService = pdfBoxOcrService;
        this.tikaOcrService = tikaOcrService;
    }

    @Override
    public String extractText(InputStream input, MediaType mediaType) throws Exception {
        if (pdfBoxOcrService != null && isPdf(mediaType)) {
            return pdfBoxOcrService.extractText(input, mediaType);
        }
        return tikaOcrService.extractText(input, mediaType);
    }

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
