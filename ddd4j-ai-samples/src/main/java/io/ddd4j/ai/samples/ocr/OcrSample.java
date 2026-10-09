package io.ddd4j.ai.samples.ocr;

import io.ddd4j.ai.extension.ocr.service.OcrService;
import org.springframework.ai.document.Document;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * ocr 文档识别组件使用示例：PDF 直提 / 多格式解析为向量库可摄入的文档列表。
 *
 * <p>前提：引入 {@code ddd4j-ai-extension-ocr}（内置 PDFBox + Tika）；图像 OCR
 * 需宿主机安装 tesseract 并开启 {@code ddd4j.ai.ocr.ocr-enabled=true}。
 *
 * @author ddd4j
 */
@Component
public class OcrSample {

    private final OcrService ocrService;

    /**
     * 构造 OcrSample。
     *
     * @param ocrService OCR 识别服务
     */
    public OcrSample(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    /**
     * 提取 PDF 内嵌文本。
     *
     * @param pdf PDF 文件
     * @return 文本结果
     * @throws Exception 执行过程中出现异常时抛出
     */
    public String extractPdfText(byte[] pdf) throws Exception {
        return ocrService.extractText(new ByteArrayInputStream(pdf), MediaType.APPLICATION_PDF);
    }

    /**
     * 解析任意格式（Office/HTML/PDF）为文档列表，可直接投喂 RAG。
     *
     * @param bytes 音频字节数据
     * @param mediaType 媒体类型
     * @return 结果列表
     * @throws Exception 执行过程中出现异常时抛出
     */
    public List<Document> extractDocuments(byte[] bytes, MediaType mediaType) throws Exception {
        return ocrService.extract(new ByteArrayInputStream(bytes), mediaType);
    }
}
