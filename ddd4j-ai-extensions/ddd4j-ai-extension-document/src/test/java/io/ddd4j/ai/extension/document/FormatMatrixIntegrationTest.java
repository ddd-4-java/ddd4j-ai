package io.ddd4j.ai.extension.document;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.ddd4j.ai.extension.document.autoconfigure.DocumentAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 格式能力矩阵端到端测试：真实 {@link DocumentReader} 上下文覆盖
 * txt / html / csv / png / wav / pdf / docx（对齐 markitdown 支持面，本地构造样本，无 Docker/外网）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@SpringBootTest(classes = DocumentAutoConfiguration.class)
class FormatMatrixIntegrationTest {

    @Autowired
    private DocumentReader reader;

    @Test
    void txt_readsContent(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("a.txt");
        Files.writeString(file, "Hello matrix\nSecond line");
        Document d = reader.read(file);
        assertThat(d.fullMarkdown()).contains("Hello matrix").contains("Second line");
        assertThat(d.mime()).isEqualTo("text/plain");
    }

    @Test
    void html_extractsStructure(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("a.html");
        Files.writeString(file, "<html><body><h1>Report</h1><h2>Sales</h2>"
                + "<table><tr><th>R</th><th>A</th></tr><tr><td>E</td><td>1</td></tr></table></body></html>");
        Document d = reader.read(file);
        assertThat(d.sections()).isNotEmpty();
        assertThat(d.sections().get(0).title()).isEqualTo("Report");
        assertThat(d.tables()).isNotEmpty();
        assertThat(d.fullMarkdown()).contains("# Report");
    }

    @Test
    void csv_readsTable(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("a.csv");
        Files.writeString(file, "name,amount\napple,3\nbanana,5");
        Document d = reader.read(file);
        assertThat(d.fullMarkdown()).contains("name").contains("amount").contains("banana");
    }

    @Test
    void png_parsesMetadata(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("a.png");
        Files.write(file, ONE_PX_PNG);
        Document d = reader.read(file);
        assertThat(d.mime()).startsWith("image/");
        assertThat(d.metadata()).containsEntry("source", "tika");
    }

    @Test
    void wav_audioBranchWithoutAsr(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("a.wav");
        Files.write(file, wavBytes());
        // 无 AsrService bean 的默认上下文：音频仅元数据，不抛异常
        Document d = reader.read(file);
        assertThat(d.mime()).startsWith("audio/");
    }

    @Test
    void pdf_readsText(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("a.pdf");
        createPdf(file.toFile(), "Invoice total 100");
        Document d = reader.read(file);
        assertThat(d.mime()).isEqualTo("application/pdf");
        assertThat(d.fullMarkdown()).contains("Invoice").contains("100");
    }

    @Test
    void docx_readsTextAndImage(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("a.docx");
        Files.write(file, minimalDocxWithImage());
        Document d = reader.read(file);
        assertThat(d.fullMarkdown()).contains("Hello embedded image");
        assertThat(d.images()).anyMatch(img -> img.src().startsWith("data:image/png;base64,"));
    }

    // ---- 本地样本构造 helpers ----

    private static final byte[] ONE_PX_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

    private static byte[] wavBytes() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] header = {
                0x52, 0x49, 0x46, 0x46, 36, 0, 0, 0, 0x57, 0x41, 0x56, 0x45,
                0x66, 0x6d, 0x74, 0x20, 16, 0, 0, 0, 1, 0, 1, 0,
                0x40, 0x1F, 0, 0, (byte) 0x80, 0x3E, 0, 0, 2, 0, 16, 0,
                0x64, 0x61, 0x74, 0x61, 8, 0, 0, 0,
                0, 0, 0, 0, (byte) 0xFF, 0x7F, 0, 0, (byte) 0xFF, 0x7F, 0, 0
        };
        out.write(header, 0, header.length);
        return out.toByteArray();
    }

    private static void createPdf(java.io.File target, String content) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(60, 720);
                for (String line : content.split("\n")) {
                    stream.showText(line);
                    stream.newLineAtOffset(0, -16);
                }
                stream.endText();
            }
            document.save(target);
        }
    }

    private static byte[] minimalDocxWithImage() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                      <Default Extension="xml" ContentType="application/xml"/>
                      <Default Extension="png" ContentType="image/png"/>
                      <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                    </Types>
                    """.getBytes());
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("_rels/.rels"));
            zip.write("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId0" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                    </Relationships>
                    """.getBytes());
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                                xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
                                xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
                                xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
                                xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
                      <w:body>
                        <w:p><w:r><w:t>Hello embedded image</w:t></w:r></w:p>
                        <w:p><w:r><w:drawing><wp:inline distT="0" distB="0" distL="0" distR="0">
                          <a:graphic><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">
                            <pic:pic><pic:blipFill><a:blip r:embed="rId1"/></pic:blipFill></pic:pic>
                          </a:graphicData></a:graphic>
                        </wp:inline></w:drawing></w:r></w:p>
                      </w:body>
                    </w:document>
                    """.getBytes());
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/_rels/document.xml.rels"));
            zip.write("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/image1.png"/>
                    </Relationships>
                    """.getBytes());
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/media/image1.png"));
            zip.write(ONE_PX_PNG);
            zip.closeEntry();
        }
        return out.toByteArray();
    }
}
