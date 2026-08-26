package io.ddd4j.ai.extension.ocr.service.impl;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PdfBoxOcrService} 单元测试：本地生成真实 PDF 验证文本直提（无需 Docker）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class PdfBoxOcrServiceTest {

    @Test
    void extractText_extractsEmbeddedPdfText() throws Exception {
        byte[] pdf = createPdfWithText("Hello Ddd4j OCR");
        try (ByteArrayInputStream input = new ByteArrayInputStream(pdf)) {
            String text = new PdfBoxOcrService().extractText(input, MediaType.APPLICATION_PDF);
            assertThat(text).contains("Hello Ddd4j OCR");
        }
    }

    @Test
    void extract_returnsSinglePdfDocument() throws Exception {
        byte[] pdf = createPdfWithText("Invoice 2026");
        try (ByteArrayInputStream input = new ByteArrayInputStream(pdf)) {
            var documents = new PdfBoxOcrService().extract(input, MediaType.APPLICATION_PDF);
            assertThat(documents).hasSize(1);
            assertThat(documents.get(0).getText()).contains("Invoice 2026");
            assertThat(documents.get(0).getMetadata()).containsEntry("sourceType", "pdf");
        }
    }

    private static byte[] createPdfWithText(String content) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(60, 720);
                stream.showText(content);
                stream.endText();
            }
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                document.save(out);
                return out.toByteArray();
            }
        }
    }
}
