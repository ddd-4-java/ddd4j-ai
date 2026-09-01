package io.ddd4j.ai.extension.document.parser;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.ddd4j.ai.extension.document.Document;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TikaDocumentParser} 嵌入资源提取测试（对齐 markitdown 的图片提取）：
 * 容器文档（docx）内嵌图片 → base64 data URL。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class TikaEmbeddedResourceTest {

    /** 1x1 透明 PNG（base64）。 */
    private static final byte[] ONE_PX_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

    @Test
    void docxWithEmbeddedImage_collectsBase64Image(@TempDir Path tmp) throws Exception {
        File file = tmp.resolve("report.docx").toFile();
        Files.write(file.toPath(), minimalDocxWithImage());

        Document document = new TikaDocumentParser().parse(file);

        assertThat(document.fullMarkdown()).contains("Hello embedded image");
        // 双通道图片：XHTML 的 embedded: 引用 + extractor 的 base64 data URL
        assertThat(document.images()).isNotEmpty();
        assertThat(document.images())
                .anyMatch(img -> img.src().startsWith("data:image/png;base64,"));
    }

    @Test
    void plainText_noEmbeddedResources(@TempDir Path tmp) throws Exception {
        File file = tmp.resolve("note.txt").toFile();
        Files.writeString(file.toPath(), "no resources");
        Document document = new TikaDocumentParser().parse(file);
        assertThat(document.images()).isEmpty();
    }

    /** 构造最小 docx：document.xml 引用 rId1 → media/image1.png。 */
    private static byte[] minimalDocxWithImage() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write("""
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
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
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId0" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                    </Relationships>
                    """.getBytes());
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write("""
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                                xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
                                xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
                                xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
                                xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
                      <w:body>
                        <w:p><w:r><w:t>Hello embedded image</w:t></w:r></w:p>
                        <w:p><w:r><w:drawing>
                          <wp:inline distT="0" distB="0" distL="0" distR="0">
                            <a:graphic><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">
                              <pic:pic><pic:blipFill><a:blip r:embed="rId1"/></pic:blipFill></pic:pic>
                            </a:graphicData></a:graphic>
                          </wp:inline>
                        </w:drawing></w:r></w:p>
                      </w:body>
                    </w:document>
                    """.getBytes());
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("word/_rels/document.xml.rels"));
            zip.write("""
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
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
