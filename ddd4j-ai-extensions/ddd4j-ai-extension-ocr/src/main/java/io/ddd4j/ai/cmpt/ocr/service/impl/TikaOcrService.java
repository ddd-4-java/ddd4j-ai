package io.ddd4j.ai.cmpt.ocr.service.impl;

import io.ddd4j.ai.cmpt.ocr.service.OcrService;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.ocr.TesseractOCRParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.ai.document.Document;
import org.springframework.http.MediaType;

import java.io.InputStream;
import java.util.List;

/**
 * 多格式解析：基于 Apache Tika 自动探测 Office / HTML / PDF / 图像等格式，
 * 提取嵌入式文本与元数据；{@code ocrEnabled} 时启用 Tesseract OCR 扫描图像。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class TikaOcrService implements OcrService {

    private final boolean ocrEnabled;

    public TikaOcrService(boolean ocrEnabled) {
        this.ocrEnabled = ocrEnabled;
    }

    @Override
    public String extractText(InputStream input, MediaType mediaType) throws Exception {
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        if (mediaType != null) {
            metadata.set(HttpHeaders.CONTENT_TYPE, mediaType.toString());
        }
        ParseContext parseContext = new ParseContext();
        if (ocrEnabled) {
            TesseractOCRConfig config = new TesseractOCRConfig();
            config.setOutputType(TesseractOCRConfig.OUTPUT_TYPE.TXT);
            parseContext.set(TesseractOCRConfig.class, config);
            parseContext.set(TesseractOCRParser.class, new TesseractOCRParser());
        }
        new AutoDetectParser().parse(input, handler, metadata, parseContext);
        return handler.toString();
    }

    @Override
    public List<Document> extract(InputStream input, MediaType mediaType) throws Exception {
        String text = extractText(input, mediaType);
        Document document = new Document.Builder()
                .text(text)
                .metadata("sourceType", "tika")
                .metadata("contentType", mediaType == null ? "unknown" : mediaType.toString())
                .build();
        return List.of(document);
    }
}
