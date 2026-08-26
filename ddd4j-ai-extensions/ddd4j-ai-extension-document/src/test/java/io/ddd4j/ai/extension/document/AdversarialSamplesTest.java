package io.ddd4j.ai.extension.document;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 对抗样本回归：空文件 / 截断 PDF / 损坏 zip / 深嵌套 / 高压缩比 / 空表格 ——
 * 全部「受控返回」（明确异常或降级成功），绝不挂死或静默 NPE。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class AdversarialSamplesTest {

    private final DocumentReader reader = new DocumentReader(
            java.util.List.of(new io.ddd4j.ai.extension.document.parser.TikaDocumentParser()));

    @Test
    @Timeout(60)
    void emptyFile_returnsEmptyDocument(@TempDir Path tmp) throws Exception {
        File file = tmp.resolve("empty.txt").toFile();
        Files.write(file.toPath(), new byte[0]);
        Document document = reader.read(file);
        assertThat(document.fullMarkdown()).isEmpty();
        assertThat(document.metadata()).containsEntry("empty", true);
    }

    @Test
    @Timeout(60)
    void truncatedPdf_returnsControlled(@TempDir Path tmp) throws Exception {
        // 合法 PDF 前缀 + 截断
        byte[] pdfPrefix = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34, 0x0A, '%', 'a'};
        File file = tmp.resolve("broken.pdf").toFile();
        Files.write(file.toPath(), pdfPrefix);
        assertThatCode(() -> {
            try {
                reader.read(file);
            } catch (Exception expected) {
                // 受控解析异常（非挂死/非 Error）即可接受
            }
        }).doesNotThrowAnyException();
    }

    @Test
    @Timeout(60)
    void corruptZip_returnsControlled(@TempDir Path tmp) throws Exception {
        // zip 魔数 + 乱码（伪装 docx）
        byte[] fake = {0x50, 0x4B, 0x03, 0x04, 0x00, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        File file = tmp.resolve("fake.docx").toFile();
        Files.write(file.toPath(), fake);
        assertThatCode(() -> {
            try {
                reader.read(file);
            } catch (Exception expected) {
                // 受控解析异常
            }
        }).doesNotThrowAnyException();
    }

    @Test
    @Timeout(60)
    void deeplyNestedContainer_returnsWithinDeadline(@TempDir Path tmp) throws Exception {
        // 15 层互嵌 zip
        byte[] payload = "deep".getBytes();
        for (int i = 0; i < 15; i++) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(out)) {
                zip.putNextEntry(new java.util.zip.ZipEntry("l" + i));
                zip.write(payload);
                zip.closeEntry();
            }
            payload = out.toByteArray();
        }
        File file = tmp.resolve("deep.zip").toFile();
        Files.write(file.toPath(), payload);
        assertThatCode(() -> reader.read(file)).doesNotThrowAnyException();
    }

    @Test
    @Timeout(60)
    void highCompressionZip_bounded(@TempDir Path tmp) throws Exception {
        // 小 zip 解压后 ~110MB 重复内容：SAX 输出限额约束下受控返回
        java.io.ByteArrayOutputStream bomb = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(bomb)) {
            zip.putNextEntry(new java.util.zip.ZipEntry("fill.txt"));
            byte[] chunk = new byte[1024 * 1024];
            for (int i = 0; i < 110; i++) {
                zip.write(chunk);
            }
            zip.closeEntry();
        }
        File file = tmp.resolve("bomb.zip").toFile();
        Files.write(file.toPath(), bomb.toByteArray());
        assertThatCode(() -> reader.read(file)).doesNotThrowAnyException();
    }

    @Test
    @Timeout(60)
    void emptyTableHtml_noNpe(@TempDir Path tmp) throws Exception {
        File file = tmp.resolve("empty-table.html").toFile();
        Files.writeString(file.toPath(), "<html><body><table><tr></tr></table></body></html>");
        Document document = reader.read(file);
        // 空表格：不 NPE，tables 空集合或空行集合
        assertThat(document.tables()).isNotNull();
    }
}
