package io.ddd4j.ai.cmpt.document.parser;

import java.io.File;

import org.junit.jupiter.api.Test;

import io.ddd4j.ai.cmpt.document.DocumentParser;
import io.ddd4j.ai.cmpt.document.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 4 组件委托解析器契约测试：类型路由 / 高优先级 / 空参校验 / 未就位降级语义。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class ComponentDelegationParsersTest {

    @Test
    void easypdf_supportsPdfWithHighPriority() {
        assertContract(new EasypdfDocumentParser(), MediaType.PDF);
    }

    @Test
    void easydoc_supportsDocxWithHighPriority() {
        assertContract(new EasydocDocumentParser(), MediaType.DOCX);
    }

    @Test
    void easyexcel_supportsXlsxWithHighPriority() {
        assertContract(new EasyexcelDocumentParser(), MediaType.XLSX);
    }

    @Test
    void easyodf_supportsUnknownWithHighPriority() {
        assertContract(new EasyodfDocumentParser(), MediaType.UNKNOWN);
    }

    @Test
    void allParsers_rejectNullFile() {
        assertThatThrownBy(() -> new EasypdfDocumentParser().parse((File) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new EasydocDocumentParser().parse((File) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new EasyexcelDocumentParser().parse((File) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new EasyodfDocumentParser().parse((File) null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void allParsers_delegationPending_throwsUnsupported() throws Exception {
        File pdf = java.nio.file.Files.createTempFile("t", ".pdf").toFile();
        try {
            assertThatThrownBy(() -> new EasypdfDocumentParser().parse(pdf))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("pending");
        } finally {
            pdf.delete();
        }
    }

    private static void assertContract(DocumentParser parser, MediaType type) {
        assertThat(parser.supports()).isEqualTo(type);
        assertThat(parser.order()).isGreaterThan(0); // 优先于通用兜底（0）
    }
}
