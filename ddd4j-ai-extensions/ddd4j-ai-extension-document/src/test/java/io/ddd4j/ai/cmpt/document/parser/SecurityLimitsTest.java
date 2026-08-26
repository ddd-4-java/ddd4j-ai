package io.ddd4j.ai.cmpt.document.parser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.ddd4j.ai.cmpt.document.Document;
import io.ddd4j.ai.cmpt.document.properties.DocumentProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@link TikaDocumentParser} 安全限额测试：深嵌套容器 / 嵌入图片数量与单图大小上限
 * （zip 炸弹与图片轰炸攻击面防护）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class SecurityLimitsTest {

    private static final byte[] ONE_PX_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

    @Test
    void deeplyNestedZip_returnsWithinDeadline(@TempDir Path tmp) throws Exception {
        byte[] nested = nestedZip(10);
        // 深嵌套容器：Tika 达到递归深度限制后停止展开，绝不挂死（60s 内返回）
        File file = tmp.resolve("nested.zip").toFile();
        Files.write(file.toPath(), nested);

        Document document = new TikaDocumentParser().parse(file);

        assertThat(document.source()).isNotNull();
    }

    @Test
    void embeddedImages_cappedAtLimit(@TempDir Path tmp) throws Exception {
        DocumentProperties properties = new DocumentProperties();
        properties.setMaxEmbeddedImages(2);
        File file = tmp.resolve("flood.docx").toFile();
        Files.write(file.toPath(), docxWithImages(3));

        Document document = new TikaDocumentParser(properties).parse(file);

        long dataUrls = document.images().stream()
                .filter(img -> img.src().startsWith("data:image/png;base64,")).count();
        assertThat(dataUrls).isLessThanOrEqualTo(2);
        assertThat(document.metadata()).containsEntry("embeddedImagesTruncated", true);
    }

    @Test
    void oversizedEmbeddedImage_skipped(@TempDir Path tmp) throws Exception {
        DocumentProperties properties = new DocumentProperties();
        properties.setMaxEmbeddedImageBytes(10); // 1x1 PNG 是 69 字节，必然超限
        File file = tmp.resolve("bigimg.docx").toFile();
        Files.write(file.toPath(), docxWithImages(1));

        Document document = new TikaDocumentParser(properties).parse(file);

        assertThat(document.fullMarkdown()).contains("Hello embedded image"); // 主文档不受影响
        assertThat(document.images()).noneMatch(img -> img.src().startsWith("data:image/png;base64,"));
        assertThat(document.metadata()).containsEntry("embeddedImagesTruncated", true);
    }

    @Test
    void zipBombHighRatio_boundedBySizeLimit(@TempDir Path tmp) throws Exception {
        // 高压缩比 zip（小包解压出大内容）：受 max-file-size 与 SAX 输出限额约束，受控返回
        ByteArrayOutputStream bomb = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bomb)) {
            zip.putNextEntry(new ZipEntry("fill.txt"));
            byte[] chunk = new byte[1024 * 1024];
            for (int i = 0; i < 110; i++) { // 解压后 110MB > 默认 100MB 限额
                zip.write(chunk);
            }
            zip.closeEntry();
        }
        DocumentProperties properties = new DocumentProperties();
        properties.setMaxFileSizeBytes(0); // 文件本身小，不触文件限额——考验 SAX 输出限额
        File file = tmp.resolve("bomb.zip").toFile();
        Files.write(file.toPath(), bomb.toByteArray());

        assertThatCode(() -> new TikaDocumentParser(properties).parse(file))
                .doesNotThrowAnyException(); // 受控（SecureContentHandler 输出限额或正常截断），绝不挂死
    }

    // ---- 样本构造 ----

    /** n 层互嵌 zip：最内层为 txt。 */
    private static byte[] nestedZip(int depth) throws Exception {
        byte[] payload = "innermost".getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < depth; i++) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(out)) {
                zip.putNextEntry(new ZipEntry("layer" + i + (i == 0 ? ".txt" : ".zip")));
                zip.write(payload);
                zip.closeEntry();
            }
            payload = out.toByteArray();
        }
        return payload;
    }

    /** 含 n 张嵌入图片的最小 docx。 */
    private static byte[] docxWithImages(int imageCount) throws Exception {
        StringBuilder rels = new StringBuilder();
        StringBuilder drawings = new StringBuilder();
        for (int i = 1; i <= imageCount; i++) {
            rels.append("<Relationship Id=\"rId").append(i)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"media/image")
                    .append(i).append(".png\"/>");
            drawings.append("<w:p><w:r><w:drawing><wp:inline><a:graphic><a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/picture\"><pic:pic><pic:blipFill><a:blip r:embed=\"rId")
                    .append(i).append("\"/></pic:blipFill></pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write(("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                      <Default Extension="xml" ContentType="application/xml"/>
                      <Default Extension="png" ContentType="image/png"/>
                      <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                    </Types>
                    """).getBytes());
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("_rels/.rels"));
            zip.write(("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId0" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                    </Relationships>
                    """).getBytes());
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                                xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
                                xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
                                xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
                                xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
                      <w:body>
                        <w:p><w:r><w:t>Hello embedded image</w:t></w:r></w:p>
                        """ + drawings + """
                      </w:body>
                    </w:document>
                    """).getBytes());
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/_rels/document.xml.rels"));
            zip.write(("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    """ + rels + """
                    </Relationships>
                    """).getBytes());
            zip.closeEntry();
            for (int i = 1; i <= imageCount; i++) {
                zip.putNextEntry(new ZipEntry("word/media/image" + i + ".png"));
                zip.write(ONE_PX_PNG);
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }
}
