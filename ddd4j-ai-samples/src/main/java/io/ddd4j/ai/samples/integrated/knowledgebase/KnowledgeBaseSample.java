package io.ddd4j.ai.samples.integrated.knowledgebase;

import io.ddd4j.ai.extension.document.DocumentReader;
import io.ddd4j.ai.extension.embedding.service.EmbeddingService;
import io.ddd4j.ai.extension.vectordb.service.VectorDbService;
import io.ddd4j.ai.extension.ocr.service.OcrService;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 场景 4：知识库管理（文档→向量化→检索，不含 LLM 对话）。
 * <p>
 * 组件串联：{@code document（解析）/ ocr（图片文字）→ embedding → vectordb}
 * <p>
 * 展示：文档解析→向量存储→相似性检索的完整数据管线。
 */
@Component
public class KnowledgeBaseSample {

    private final DocumentReader documentReader;
    private final OcrService ocrService;
    private final EmbeddingService embeddingService;
    private final VectorDbService vectorDbService;

    /**
     * 构造 KnowledgeBaseSample。
     *
     * @param documentReader 文档读取服务
     * @param ocrService OCR 识别服务
     * @param embeddingService 向量嵌入服务
     * @param vectorDbService 向量库服务
     */
    public KnowledgeBaseSample(DocumentReader documentReader, OcrService ocrService,
                               EmbeddingService embeddingService, VectorDbService vectorDbService) {
        this.documentReader = documentReader;
        this.ocrService = ocrService;
        this.embeddingService = embeddingService;
        this.vectorDbService = vectorDbService;
    }

    /**
     * 文档入库：解析文件 → 提取文本 → 向量化 → 存入向量库。
     *
     * @param file 文件
     * @throws Exception 执行过程中出现异常时抛出
     */
    public void ingestFile(File file) throws Exception {
        var doc = documentReader.read(file);
        vectorDbService.add(List.of(new Document(doc.fullMarkdown(),
                Map.of("source", file.getName()))));
    }

    /**
     * 批量文档入库。
     *
     * @param files 文件列表
     * @throws Exception 执行过程中出现异常时抛出
     */
    public void ingestFiles(List<File> files) throws Exception {
        for (File file : files) {
            ingestFile(file);
        }
    }

    /**
     * 图片 OCR 入库：提取图片中的文字 → 向量化 → 存入向量库。
     *
     * @param imageStream 图片输入流
     * @throws Exception 执行过程中出现异常时抛出
     */
    public void ingestImage(InputStream imageStream) throws Exception {
        String text = ocrService.extractText(imageStream, null);
        if (text != null && !text.isBlank()) {
            vectorDbService.add(List.of(new Document(text, Map.of("source", "ocr-image"))));
        }
    }

    /**
     * 知识检索：返回最相关的文档片段（topK 可配）。
     *
     * @param query 检索问题
     * @param topK 返回条数上限
     * @return 结果列表
     */
    public List<Document> search(String query, int topK) {
        return vectorDbService.search(query, topK, null);
    }

    /**
     * 获取向量维度信息。
     *
     * @return 数值结果
     */
    public int embeddingDimensions() {
        return embeddingService.dimensions();
    }
}
