package io.ddd4j.ai.cmpt.document;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link Document} 统一结构模型与 {@link MediaType} 路由契约测试。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class DocumentTest {

    @Test
    void documentRecordCarriesFieldsAndSource() {
        DocumentSection s = new DocumentSection("标题", 1, "x", List.of(), List.of(), List.of());
        Document d = new Document("t", "application/pdf", SourceType.EASYPDF,
                List.of(s), List.of(), List.of(), "# t\n\n## 标题\n\nx", Map.of("pages", 1));
        assertThat(d.title()).isEqualTo("t");
        assertThat(d.source()).isEqualTo(SourceType.EASYPDF);
        assertThat(d.fullMarkdown()).contains("# t").contains("## 标题");
        assertThat(d.metadata()).containsEntry("pages", 1);
    }

    @Test
    void documentRecord_defensivelyCopiesCollections() {
        java.util.ArrayList<DocumentSection> sections = new java.util.ArrayList<>();
        sections.add(new DocumentSection("s", 1, "c", List.of(), List.of(), List.of()));
        Document d = new Document("t", "m", SourceType.MARKITDOWN4J, sections, List.of(), List.of(), "md", null);
        sections.clear();
        assertThat(d.sections()).hasSize(1);
        assertThat(d.metadata()).isEmpty();
    }

    @Test
    void mediaTypeDetectsFromFilename() {
        assertThat(MediaType.fromFilename("a.PDF")).isEqualTo(MediaType.PDF);
        assertThat(MediaType.fromFilename("b.docx")).isEqualTo(MediaType.DOCX);
        assertThat(MediaType.fromFilename("c.xlsx")).isEqualTo(MediaType.XLSX);
        assertThat(MediaType.fromFilename("d.ofd")).isEqualTo(MediaType.UNKNOWN); // OFD 留给 easyodf
        assertThat(MediaType.fromFilename("e.pptx")).isEqualTo(MediaType.PPTX);
        assertThat(MediaType.fromFilename("f.csv")).isEqualTo(MediaType.CSV);
        assertThat(MediaType.fromFilename("g.ipynb")).isEqualTo(MediaType.IPYNB);
        assertThat(MediaType.fromFilename("h.txt")).isEqualTo(MediaType.PLAINTEXT);
        assertThat(MediaType.fromFilename("i.md")).isEqualTo(MediaType.MD);
        assertThat(MediaType.fromFilename("j.png")).isEqualTo(MediaType.IMAGE);
        assertThat(MediaType.fromFilename(null)).isEqualTo(MediaType.UNKNOWN);
    }

    @Test
    void documentTable_normalizesNullCollections() {
        DocumentTable t = new DocumentTable(null, Arrays.asList(Arrays.asList("a", "b")));
        assertThat(t.headers()).isEmpty();
        assertThat(t.rows()).hasSize(1);
    }
}
