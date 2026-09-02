package io.ddd4j.ai.samples.integrated.docunderstanding;

import io.ddd4j.ai.extension.chat.service.ChatService;
import io.ddd4j.ai.extension.document.DocumentReader;
import io.ddd4j.ai.extension.ocr.service.OcrService;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;

/**
 * 场景 6：文档理解（document + ocr + chat + agent）。
 * <p>
 * 展示：PDF/Office/图片上传 → 文本提取（含 OCR）→ 智能问答。
 */
@Component
public class DocUnderstandingSample {

    private final DocumentReader documentReader;
    private final OcrService ocrService;
    private final ChatService chatService;

    public DocUnderstandingSample(DocumentReader documentReader, OcrService ocrService,
                                  ChatService chatService) {
        this.documentReader = documentReader;
        this.ocrService = ocrService;
        this.chatService = chatService;
    }

    /**
     * 文档问答：读取文件内容 → 拼接为提问上下文 → LLM 回答。
     */
    public String askAboutPdf(File file, String question) throws Exception {
        var doc = documentReader.read(file);
        String prompt = "以下是文档内容：\n\n" + doc.fullMarkdown()
                + "\n\n请根据文档回答：" + question;
        return chatService.chat(prompt);
    }

    /**
     * 图片 OCR + 文字问答：提取图片文字 → 拼接提问 → LLM 回答。
     */
    public String extractAndAsk(InputStream image, String question) throws Exception {
        String text = ocrService.extractText(image, null);
        String prompt = "以下是图片中提取的文字：\n\n" + text
                + "\n\n请根据这些文字回答：" + question;
        return chatService.chat(prompt);
    }

    /**
     * 文档摘要：读取文件 → 生成摘要（自动以"请总结"为提问）。
     */
    public String summarizeDocument(File file) throws Exception {
        var doc = documentReader.read(file);
        return chatService.chat("请为以下文档生成简洁摘要：\n\n" + doc.fullMarkdown());
    }
}
