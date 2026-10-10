package io.ddd4j.ai.extension.ocr.service.impl;

import io.ddd4j.ai.extension.ocr.service.OcrService;
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

    /**
     * 构造 Tika 识别服务。
     *
     * @param ocrEnabled 是否启用 Tesseract OCR 图像扫描（依赖宿主机 tesseract 可执行文件）
     */
    public TikaOcrService(boolean ocrEnabled) {
        this.ocrEnabled = ocrEnabled;
    }

    /**
     * Tika 自动探测格式并提取纯文本；开启 OCR 时对图像追加 Tesseract 扫描。
     *
     * @param input     文档内容流
     * @param mediaType 媒体类型提示（null 时交由 Tika 自行探测）
     * @return 提取的纯文本
     * @throws Exception 解析失败时原样抛出
     */
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

    /**
     * 将 Tika 提取结果包装为统一文档模型（单文档，附 sourceType/contentType 元数据）。
     *
     * @param input     文档内容流
     * @param mediaType 媒体类型提示（null 时 contentType 记为 unknown）
     * @return 单元素文档模型列表
     * @throws Exception 解析失败时原样抛出
     */
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
