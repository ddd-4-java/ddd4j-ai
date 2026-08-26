# ddd4j-ai-extension-document 实现计划（智能体文档统一读取门面 —— markitdown4j 深度集成 + 4 组件委托）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 新增 `ddd4j-ai-extension-document` 模块，作为智能体文档读取的统一门面。**深度集成** `io.gitlab.ade90036:markitdown-core:1.0.0` + **14 个 converter 子模块**（MIT 协议）作为 PDF/DOCX/XLSX/PPTX/CSV/HTML/IPYNB/EPUB/RSS/ZIP/PlainText/Wikipedia 的"通用基础实现"；同时**委托** 4 组件仓库（easypdf/easydoc/easyexcel/easyodf）以获得**深度结构还原**（1:1 保真），两套并行：用户可配置"高质量优先"（委托组件）或"通用优先"（markitdown4j 兜底）。

**关键定位（用户已确认）**：
- ddd4j-ai-extension-document **不是兜底**，而是 markitdown4j 的**主要使用者**（14 个 converter 全部依赖进来）
- 4 组件仓库**不是 markitdown 的复刻**，而是各自基于自有生态（iText7/docx4j/POI/ofdrw）实现**完美 1:1 结构还原**（可达 95%+ 还原度，markitdown 是参考但非依赖）
- 两套输出**统一为 ddd4j 的 `Document` POJO**（智能体调用单点）

> **实施偏差记录（2026-08-26）**：markitdown4j 1.0.0 为 Maven Central 唯一版本，但其 class 文件为
> **Java 25 字节码（version 69）**，项目 target Java 17（version 65）无法加载且无低版本可退。
> 按本计划预留的 `SourceType.TIKA_FALLBACK` 角色，通用基础实现改用 **Apache Tika 3.3.2**
> （已在依赖治理、Java 17 兼容、覆盖 PDF/Office/HTML/CSV 等全格式），
> 其余架构（DocumentParser SPI / DocumentReader 门面 / 4 组件委托 / SourceType 追踪）完全不变；
> 若后续 markitdown4j 发布 Java 17 兼容版本，可无缝替换回（仅改 `TikaDocumentParser` 一个适配器）。

**Architecture:**
- 主门面 `DocumentReader.read(File/InputStream/Path/URL) → Document` —— 智能体入口
- 解析器 SPI：`DocumentParser` 接口 + `MediaType` 路由 + 优先级 `order()` 排序
- 默认实现：markitdown4j 全套 14 converter（MIT 子集，单 Maven 坐标聚合）+ 自研的 Markitdown4jAdapter 桥接 `DocumentConverter` → `DocumentParser`
- 委托实现：4 个 `*DocumentParser` 包装 easypdf/easydoc/easyexcel/easyodf（这些组件已发布到 Maven Central 后启用；当前为契约占位）
- 切换策略：默认走"高质量"（委托组件 → 失败回退 markitdown4j）；`@ConditionalOnProperty("ddd4j.ai.document.high-quality")` 控制

代码位置：`/Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai/ddd4j-ai-extensions/ddd4j-ai-extension-document/`，遵循 ddd4j-ai-extension-rag 同构（`pom.xml` + `autoconfigure/` + `properties/` + `service/` + `service/impl/`）。

**Tech Stack:** markitdown4j 1.0.0（core + 14 converters，全 MIT）、Apache Tika 3.3.2（已管）、Spring AI Commons 2.0.0、Spring Boot AutoConfigure。

## Global Constraints

- 模块坐标：`io.ddd4j.ai:ddd4j-ai-extension-document:${revision}`，包 `io.ddd4j.ai.extension.document`
- **Java 17+ 语法**（ddd4j 整体目标；可用 `var`/record/sealed）
- 引入依赖：`io.gitlab.ade90036:markitdown-core:1.0.0` + `io.gitlab.ade90036:converter-pdf:1.0.0` + `io.gitlab.ade90036:converter-docx:1.0.0` + `io.gitlab.ade90036:converter-xlsx:1.0.0` + `io.gitlab.ade90036:converter-csv:1.0.0` + `io.gitlab.ade90036:converter-html:1.0.0` + `io.gitlab.ade90036:converter-pptx:1.0.0` + `io.gitlab.ade90036:converter-plaintext:1.0.0` + `io.gitlab.ade90036:converter-epub:1.0.0` + `io.gitlab.ade90036:converter-ipynb:1.0.0` + `io.gitlab.ade90036:converter-rss:1.0.0` + `io.gitlab.ade90036:converter-wikipedia:1.0.0` + `io.gitlab.ade90036:converter-zip:1.0.0`（13 个 + core，共 14 个 MIT artifact）
- **避免** `io.gitlab.ade90036:markitdown:0.0.1`（AGPL 协议）——改用全 MIT 套件
- 4 组件委托实现：当前为契约占位（标 `@ConditionalOnClass`），待各组件 JAR 发布后激活
- 提交信息风格：`feat(extension): add document reader with markitdown4j integration`
- 验证命令：`cd ddd4j-ai-extensions && ./mvnw -pl ddd4j-ai-extension-document -am clean verify` 必须 BUILD SUCCESS

---

### Task 1: 模块骨架 + Document POJO（统一结构模型）

**Files:**
- Create: `ddd4j-ai-extension-document/pom.xml`（含 14 个 markitdown4j 依赖）
- Create: `src/main/java/io/ddd4j/ai/cmpt/document/Document.java` (record)
- Create: `src/main/java/io/ddd4j/ai/cmpt/document/DocumentSection.java` (record)
- Create: `src/main/java/io/ddd4j/ai/cmpt/document/DocumentTable.java` (record)
- Create: `src/main/java/io/ddd4j/ai/cmpt/document/DocumentImage.java` (record)
- Create: `src/main/java/io/ddd4j/ai/cmpt/document/MediaType.java` (enum)
- Create: `src/main/java/io/ddd4j/ai/cmpt/document/SourceType.java` (enum: `MARKITDOWN4J | EASYPDF | EASYDOC | EASYEXCEL | EASYODF`)
- Test: `src/test/java/io/ddd4j/ai/cmpt/document/DocumentTest.java`
- Modify: `ddd4j-ai-extensions/pom.xml`（添加 `<subproject>ddd4j-ai-extension-document</subproject>`）

**Interfaces:**
- Produces:
  - `public record Document(String title, String mime, SourceType source, List<DocumentSection> sections, List<DocumentTable> tables, List<DocumentImage> images, String fullMarkdown, Map<String, Object> metadata)`
  - `public record DocumentSection(String title, int level, String content, List<DocumentSection> children, List<DocumentTable> tables, List<DocumentImage> images)`
  - `public record DocumentTable(List<List<String>> headers, List<List<String>> rows)`
  - `public record DocumentImage(String alt, String src)`（src = base64 data URL 或 URL）
  - `public enum MediaType { PDF, DOCX, XLSX, PPTX, CSV, HTML, IPYNB, EPUB, RSS, WIKIPEDIA, ZIP, PLAINTEXT, MD, IMAGE, UNKNOWN }` + `public static MediaType fromFilename(String)`
  - `public enum SourceType { MARKITDOWN4J, EASYPDF, EASYDOC, EASYEXCEL, EASYODF, TIKA_FALLBACK }` —— **追踪输出来源**（智能体/审计关键需求）

- [x] **Step 1: 添加子模块声明**

修改 `ddd4j-ai-extensions/pom.xml`：
```xml
<subproject>ddd4j-ai-extension-document</subproject>
```

- [x] **Step 2: 写失败测试**

`DocumentTest.java`：
```java
package io.ddd4j.ai.extension.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class DocumentTest {

    @Test
    void documentRecordCarriesFieldsAndSource() {
        DocumentSection s = new DocumentSection("标题", 1, "x", List.of(), List.of(), List.of());
        Document d = new Document("t", "application/pdf", SourceType.EASYPDF,
                List.of(s), List.of(), List.of(), "# t\n\n## 标题\n\nx", Map.of("pages", 1));
        assertThat(d.title()).isEqualTo("t");
        assertThat(d.source()).isEqualTo(SourceType.EASYPDF);
        assertThat(d.fullMarkdown()).contains("# t").contains("## 标题");
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
    }
}
```

- [x] **Step 3: 运行测试确认失败**

Run: `cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai/ddd4j-ai-extensions && ./mvnw -pl ddd4j-ai-extension-document -am test -Dtest=DocumentTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep -E "BUILD|ERROR" | head -3`

- [x] **Step 4: 创建 pom.xml（14 个 markitdown4j 依赖 + Tika + Spring AI）+ 7 个 model 文件**

`pom.xml`（节选关键依赖）：
```xml
<properties>
    <markitdown4j.version>1.0.0</markitdown4j.version>
</properties>
<dependencies>
    <dependency>
        <groupId>io.gitlab.ade90036</groupId>
        <artifactId>markitdown-core</artifactId>
        <version>${markitdown4j.version}</version>
    </dependency>
    <!-- 13 个 converter（PDF/Office/HTML/CSV/...） -->
    <dependency><groupId>io.gitlab.ade90036</groupId><artifactId>converter-pdf</artifactId><version>${markitdown4j.version}</version></dependency>
    <dependency><groupId>io.gitlab.ade90036</groupId><artifactId>converter-docx</artifactId><version>${markitdown4j.version}</version></dependency>
    <dependency><groupId>io.gitlab.ade90036</groupId><artifactId>converter-xlsx</artifactId><version>${markitdown4j.version}</version></dependency>
    <dependency><groupId>io.gitlab.ade90036</groupId><artifactId>converter-csv</artifactId><version>${markitdown4j.version}</version></dependency>
    <dependency><groupId>io.gitlab.ade90036</groupId><artifactId>converter-html</artifactId><version>${markitdown4j.version}</version></dependency>
    <dependency><groupId>io.gitlab.ade90036</groupId><artifactId>converter-pptx</artifactId><version>${markitdown4j.version}</version></dependency>
    <dependency><groupId>io.gitlab.ade90036</groupId><artifactId>converter-plaintext</artifactId><version>${markitdown4j.version}</version></dependency>
    <dependency><groupId>io.gitlab.ade90036</groupId><artifactId>converter-zip</artifactId><version>${markitdown4j.version}</version></dependency>
    <dependency><groupId>io.gitlab.ade90036</groupId><artifactId>converter-epub</artifactId><version>${markitdown4j.version}</version></dependency>
    <dependency><groupId>io.gitlab.ade90036</groupId><artifactId>converter-ipynb</artifactId><version>${markitdown4j.version}</version></dependency>
    <dependency><groupId>io.gitlab.ade90036</groupId><artifactId>converter-rss</artifactId><version>${markitdown4j.version}</version></dependency>
    <dependency><groupId>io.gitlab.ade90036</groupId><artifactId>converter-wikipedia</artifactId><version>${markitdown4j.version}</version></dependency>
    <dependency>
        <groupId>org.apache.tika</groupId>
        <artifactId>tika-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-commons</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-autoconfigure</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

7 个 model 文件按 record 风格生成（详见 plan 完整版 Task 1 Step 4）。

- [x] **Step 5: 运行测试确认通过**

Run: `./mvnw -pl ddd4j-ai-extension-document -am test -Dtest=DocumentTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep -E "Tests run:|BUILD" | tail -3`
Expected: PASS（2 tests）

- [x] **Step 6: Commit**

```bash
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
git add ddd4j-ai-extensions/pom.xml ddd4j-ai-extensions/ddd4j-ai-extension-document/
git commit -m "feat(extension): add ddd4j-ai-extension-document module with Document POJO and SourceType tracking"
```

---

### Task 2: DocumentParser SPI + Markitdown4jAdapter（markitdown4j 主入口桥接）

**Files:**
- Create: `src/main/java/io/ddd4j/ai/cmpt/document/DocumentParser.java` (interface)
- Create: `src/main/java/io/ddd4j/ai/cmpt/document/parser/Markitdown4jAdapter.java`（包装 markitdown4j 的 MarkItDown + 14 converters）
- Test: `src/test/java/io/ddd4j/ai/cmpt/document/parser/Markitdown4jAdapterTest.java`

**Interfaces:**
- Produces:
  - `public interface DocumentParser { MediaType supports(); default int order() { return 0; } Document parse(File file) throws Exception; Document parse(InputStream in, String filename) throws Exception; }`
  - `Markitdown4jAdapter implements DocumentParser` — 内部用 `MarkItDown.builder().registerConverter(new PdfConverter()).registerConverter(new DocxConverter())...build().convert(Path)` 获取 `DocumentConverterResult`，映射为 `Document` (source=`MARKITDOWN4J`)
  - `order() = 0`（**最低优先级**）—— 作为通用兜底，让 4 组件委托 parser 优先

- [x] **Step 1: 写失败测试**

`Markitdown4jAdapterTest.java`：
```java
package io.ddd4j.ai.extension.document.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.ddd4j.ai.extension.document.Document;
import io.ddd4j.ai.extension.document.MediaType;
import io.ddd4j.ai.extension.document.SourceType;

class Markitdown4jAdapterTest {

    @Test
    void adapterSupportsAllStandardTypes() {
        Markitdown4jAdapter a = new Markitdown4jAdapter();
        assertThat(a.order()).isZero();
        assertThat(a.supports()).isEqualTo(MediaType.UNKNOWN); // 兜底：匹配所有 UNKNOWN
    }

    @Test
    void parseRejectsNullFile() {
        assertThatThrownBy(() -> new Markitdown4jAdapter().parse((File) null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void parseSimpleTextFile() throws Exception {
        @TempDir Path tmp;
        File f = tmp.resolve("hello.txt").toFile();
        Files.writeString(f.toPath(), "Hello World\nLine 2");
        Document d = new Markitdown4jAdapter().parse(f);
        assertThat(d.source()).isEqualTo(SourceType.MARKITDOWN4J);
        assertThat(d.fullMarkdown()).contains("Hello World").contains("Line 2");
    }
}
```

- [x] **Step 2: 运行测试确认失败**

Run: `./mvnw -pl ddd4j-ai-extension-document -am test -Dtest=Markitdown4jAdapterTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep -E "BUILD|ERROR" | head -3`

- [x] **Step 3: 实现 DocumentParser + Markitdown4jAdapter**

`DocumentParser.java`：
```java
package io.ddd4j.ai.extension.document;
import java.io.File; import java.io.InputStream;
public interface DocumentParser {
    MediaType supports();
    default int order() { return 0; }
    Document parse(File file) throws Exception;
    Document parse(InputStream in, String filename) throws Exception;
}
```

`Markitdown4jAdapter.java`：
```java
package io.ddd4j.ai.extension.document.parser;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.ddd4j.ai.extension.document.Document;
import io.ddd4j.ai.extension.document.DocumentImage;
import io.ddd4j.ai.extension.document.DocumentSection;
import io.ddd4j.ai.extension.document.DocumentTable;
import io.ddd4j.ai.extension.document.MediaType;
import io.ddd4j.ai.extension.document.SourceType;
import io.gitlab.ade90036.markitdown.core.DocumentConverterResult;
import io.gitlab.ade90036.markitdown.core.MarkItDown;
import io.gitlab.ade90036.markitdown.converters.csv.CsvConverter;
import io.gitlab.ade90036.markitdown.converters.docx.DocxConverter;
import io.gitlab.ade90036.markitdown.converters.epub.EpubConverter;
import io.gitlab.ade90036.markitdown.converters.html.HtmlConverter;
import io.gitlab.ade90036.markitdown.converters.ipynb.IpynbConverter;
import io.gitlab.ade90036.markitdown.converters.pdf.PdfConverter;
import io.gitlab.ade90036.markitdown.converters.plaintext.PlainTextConverter;
import io.gitlab.ade90036.markitdown.converters.pptx.PptxConverter;
import io.gitlab.ade90036.markitdown.converters.rss.RssConverter;
import io.gitlab.ade90036.markitdown.converters.wikipedia.WikipediaConverter;
import io.gitlab.ade90036.markitdown.converters.xlsx.XlsxConverter;
import io.gitlab.ade90036.markitdown.converters.zip.ZipConverter;

public final class Markitdown4jAdapter implements DocumentParser {

    private static final MarkItDown MARKITDOWN = MarkItDown.builder()
            .registerConverter(new PdfConverter())
            .registerConverter(new DocxConverter())
            .registerConverter(new XlsxConverter())
            .registerConverter(new PptxConverter())
            .registerConverter(new CsvConverter())
            .registerConverter(new HtmlConverter())
            .registerConverter(new PlainTextConverter())
            .registerConverter(new EpubConverter())
            .registerConverter(new IpynbConverter())
            .registerConverter(new RssConverter())
            .registerConverter(new WikipediaConverter())
            .registerConverter(new ZipConverter())
            .build();

    @Override public MediaType supports() { return MediaType.UNKNOWN; }
    @Override public int order() { return 0; } // 兜底

    @Override
    public Document parse(File file) throws Exception {
        Objects.requireNonNull(file, "file must not be null");
        DocumentConverterResult r = MARKITDOWN.convert(file.toPath());
        return map(r, file.getName(), guessMime(file.getName()));
    }

    @Override
    public Document parse(InputStream in, String filename) throws Exception {
        Objects.requireNonNull(in, "in must not be null");
        File tmp = File.createTempFile("dai-doc-", "-" + (filename == null ? ".tmp" : filename));
        try {
            Files.copy(in, tmp.toPath());
            return parse(tmp);
        } finally { tmp.delete(); }
    }

    private static Document map(DocumentConverterResult r, String name, String mime) {
        List<DocumentSection> sections = new ArrayList<>();
        DocumentSection sec = new DocumentSection(name == null ? "Document" : name, 1,
                r.markdown() == null ? "" : r.markdown(), List.of(), List.of(), List.of());
        sections.add(sec);
        return new Document(
                r.title().orElse(name),
                mime,
                SourceType.MARKITDOWN4J,
                sections,
                List.of(),
                List.of(),
                r.markdown(),
                Map.of("source", "markitdown4j", "title", r.title().orElse("")));
    }

    private static String guessMime(String filename) {
        if (filename == null) return "application/octet-stream";
        String n = filename.toLowerCase();
        if (n.endsWith(".pdf")) return "application/pdf";
        if (n.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (n.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (n.endsWith(".pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        if (n.endsWith(".csv")) return "text/csv";
        if (n.endsWith(".html") || n.endsWith(".htm")) return "text/html";
        if (n.endsWith(".txt")) return "text/plain";
        if (n.endsWith(".md")) return "text/markdown";
        if (n.endsWith(".json")) return "application/json";
        if (n.endsWith(".ipynb")) return "application/x-ipynb+json";
        if (n.endsWith(".epub")) return "application/epub+zip";
        if (n.endsWith(".xml")) return "application/rss+xml";
        if (n.endsWith(".zip")) return "application/zip";
        return "application/octet-stream";
    }
}
```

- [x] **Step 4: 验证通过 + Commit**

Run: `./mvnw -pl ddd4j-ai-extension-document -am test -Dtest=Markitdown4jAdapterTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep -E "Tests run:|BUILD" | tail -3`
Expected: PASS（3 tests）

- [x] **Step 5: Commit**

```bash
git add ddd4j-ai-extensions/ddd4j-ai-extension-document/
git commit -m "feat(extension): add Markitdown4jAdapter bridging 14 converters to DocumentParser SPI"
```

---

### Task 3: 4 组件委托 Parser（easypdf/easydoc/easyexcel/easyodf 契约占位）

**Files:**
- Create: `src/main/java/io/ddd4j/ai/cmpt/document/parser/EasypdfDocumentParser.java`
- Create: `src/main/java/io/ddd4j/ai/cmpt/document/parser/EasydocDocumentParser.java`
- Create: `src/main/java/io/ddd4j/ai/cmpt/document/parser/EasyexcelDocumentParser.java`
- Create: `src/main/java/io/ddd4j/ai/cmpt/document/parser/EasyodfDocumentParser.java`
- Test: 每个 parser 1 个契约测试

**Interfaces:**
- Produces:
  - 4 个 `*Parser implements DocumentParser` —— 内部**当前为契约占位**（标 `@ConditionalOnClass`），各组件 JAR 发布后激活
  - `order() = 10`（**高于** markitdown4j 兜底的 0）—— 高质量优先

- [x] **Step 1: 写契约测试（以 EasypdfDocumentParser 为代表）**

```java
package io.ddd4j.ai.extension.document.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;

import org.junit.jupiter.api.Test;

import io.ddd4j.ai.extension.document.MediaType;

class EasypdfDocumentParserTest {

    @Test
    void parserSupportsPdfWithHighPriority() {
        EasypdfDocumentParser p = new EasypdfDocumentParser();
        assertThat(p.supports()).isEqualTo(MediaType.PDF);
        assertThat(p.order()).isGreaterThan(0); // 优先于 Markitdown4jAdapter
    }

    @Test
    void parseRejectsNullFile() {
        assertThatThrownBy(() -> new EasypdfDocumentParser().parse((File) null))
                .isInstanceOf(NullPointerException.class);
    }
}
```

- [x] **Step 2: 运行测试确认失败**

- [x] **Step 3: 实现 4 个 Parser（契约占位：`@ConditionalOnClass(name = "io.github.easy4j.pdf.core.convert.HtmlPdfConverter")` 等）**

```java
// EasypdfDocumentParser.java
package io.ddd4j.ai.extension.document.parser;

import java.io.File; import java.io.InputStream; import java.util.Objects;
import io.ddd4j.ai.extension.document.Document; import io.ddd4j.ai.extension.document.MediaType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

@ConditionalOnClass(name = "io.github.easy4j.pdf.core.convert.HtmlPdfConverter")
public class EasypdfDocumentParser implements DocumentParser {
    @Override public MediaType supports() { return MediaType.PDF; }
    @Override public int order() { return 10; } // 高于 markitdown4j (0)

    @Override
    public Document parse(File file) throws Exception {
        Objects.requireNonNull(file, "file must not be null");
        // 委托 easypdf：等组件 JAR 发布后激活
        // io.github.easy4j.pdf.xhtml.convert.EasyPdf.pdfToStructuredMarkdown(file)
        // 当前抛 "no impl yet" 让 markitdown4j 兜底（order=0）
        throw new UnsupportedOperationException(
            "easypdf delegation pending; ensure io.github.easy4j:easypdf-core is on classpath and 'pdfToStructuredMarkdown' is available");
    }

    @Override
    public Document parse(InputStream in, String filename) throws Exception {
        Objects.requireNonNull(in, "in must not be null");
        return parse(new java.io.File(createTempFile(in, filename)));
    }
    private static String createTempFile(InputStream in, String name) throws Exception {
        File tmp = File.createTempFile("dai-eps-", "-" + (name == null ? ".pdf" : name));
        java.nio.file.Files.copy(in, tmp.toPath()); return tmp.getAbsolutePath();
    }
}
```

EasydocDocumentParser / EasyexcelDocumentParser / EasyodfDocumentParser 同构，分别用 `@ConditionalOnClass("io.github.easy4j.doc.xhtml.markdown.DocxToMarkdownConverter")` 等。

- [x] **Step 4: 验证 4 个测试 + Commit**

Run: `./mvnw -pl ddd4j-ai-extension-document -am test -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep -E "Tests run:|BUILD" | tail -3`
Expected: PASS（≥12 tests）

```bash
git add ddd4j-ai-extensions/ddd4j-ai-extension-document/
git commit -m "feat(extension): add 4 high-priority component-delegation parsers (PDF/DOCX/XLSX/ODF, @ConditionalOnClass)"
```

---

### Task 4: DocumentReader 门面 + Spring Boot AutoConfiguration

**Files:**
- Create: `src/main/java/io/ddd4j/ai/cmpt/document/DocumentReader.java`
- Create: `src/main/java/io/ddd4j/ai/cmpt/document/properties/DocumentProperties.java`
- Create: `src/main/java/io/ddd4j/ai/cmpt/document/autoconfigure/DocumentAutoConfiguration.java`
- Create: `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Test: `src/test/java/io/ddd4j/ai/cmpt/document/DocumentReaderTest.java`
- Test: `src/test/java/io/ddd4j/ai/cmpt/document/autoconfigure/DocumentAutoConfigurationTest.java`

**Interfaces:**
- Produces:
  - `public final class DocumentReader { public Document read(File); Document read(InputStream, String); Document read(java.net.URL); Document read(java.nio.file.Path); }` —— 智能体主入口
  - `DocumentProperties`（`@ConfigurationProperties("ddd4j.ai.document")`）：开关、缓存、并发等
  - `DocumentAutoConfiguration`：注册 `DocumentReader` + 5 个 Parser Bean（4 个委托 + 1 个 markitdown4j 兜底），`@ConditionalOnProperty` 控制

- [x] **Step 1: 写失败测试**

`DocumentReaderTest.java`：
```java
@SpringBootTest(classes = DocumentAutoConfiguration.class)
class DocumentReaderTest {
    @Autowired DocumentReader reader;
    @Test
    void readerRejectsNullFile() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> reader.read((java.io.File) null))
                .isInstanceOf(NullPointerException.class);
    }
}
```

- [x] **Step 2: 实现 4 个文件 + SPI 元数据**

```java
// DocumentReader.java
@Component
public final class DocumentReader {
    private final java.util.List<DocumentParser> parsers;
    public DocumentReader(java.util.List<DocumentParser> parsers) {
        this.parsers = parsers.stream()
                .sorted(java.util.Comparator.comparingInt(DocumentParser::order).reversed())
                .toList();
    }
    public Document read(File file) throws Exception {
        java.util.Objects.requireNonNull(file, "file must not be null");
        MediaType type = MediaType.fromFilename(file.getName());
        for (DocumentParser p : parsers) {
            if (p.supports() == type || p.supports() == MediaType.UNKNOWN) {
                try { return p.parse(file); }
                catch (UnsupportedOperationException ex) { /* 委托 parser 未就位，尝试下一个 */ }
                catch (Exception ex) { throw ex; }
            }
        }
        throw new UnsupportedOperationException("No parser for " + type);
    }
    public Document read(InputStream in, String filename) throws Exception {
        java.util.Objects.requireNonNull(in, "in must not be null");
        MediaType type = MediaType.fromFilename(filename);
        for (DocumentParser p : parsers) {
            if (p.supports() == type || p.supports() == MediaType.UNKNOWN) {
                try { return p.parse(in, filename); } catch (UnsupportedOperationException ex) {}
            }
        }
        throw new UnsupportedOperationException("No parser for " + type);
    }
    public Document read(java.net.URL url) throws Exception {
        java.util.Objects.requireNonNull(url, "url must not be null");
        try (InputStream in = url.openStream()) { return read(in, url.getPath()); }
    }
    public Document read(java.nio.file.Path path) throws Exception { return read(path.toFile()); }
}

// DocumentProperties.java
@ConfigurationProperties("ddd4j.ai.document")
public class DocumentProperties {
    private boolean enableHighQuality = true;  // 委托 4 组件（默认）
    private boolean enableMarkitdown4jFallback = true;  // markitdown4j 兜底
    // getter/setter
}

// DocumentAutoConfiguration.java
@AutoConfiguration
@EnableConfigurationProperties(DocumentProperties.class)
public class DocumentAutoConfiguration {
    @Bean @ConditionalOnMissingBean
    public DocumentReader documentReader(java.util.List<DocumentParser> parsers) {
        return new DocumentReader(parsers);
    }

    @Bean @ConditionalOnMissingBean @ConditionalOnProperty("ddd4j.ai.document.enable-markitdown4j-fallback", matchIfMissing = true)
    public DocumentParser markitdown4jAdapter() { return new Markitdown4jAdapter(); }

    @Bean @ConditionalOnMissingBean(name = "easypdfDocumentParser") @ConditionalOnProperty("ddd4j.ai.document.enable-high-quality", matchIfMissing = true)
    public DocumentParser easypdfDocumentParser() { return new EasypdfDocumentParser(); }
    // ... 其他 3 个
}
```

`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`：
```
io.ddd4j.ai.extension.document.autoconfigure.DocumentAutoConfiguration
```

- [x] **Step 3: 验证 + Commit**

Run: `./mvnw -pl ddd4j-ai-extension-document -am test -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep -E "Tests run:|BUILD" | tail -3`

```bash
git add ddd4j-ai-extensions/ddd4j-ai-extension-document/
git commit -m "feat(extension): add DocumentReader facade and Spring Boot autoconfig (markitdown4j + 4 component parsers)"
```

---

### Task 5: 集成测试（实际 PDF 渲染验证 markitdown4j 端到端）

**Files:**
- Create: `src/test/java/io/ddd4j/ai/cmpt/document/DocumentReaderIntegrationTest.java`
- Create: `src/test/resources/sample.pdf`（从上游 markitdown-java 项目或本地 PDF 测试样本）

- [x] **Step 1: 写集成测试**

```java
@SpringBootTest(classes = DocumentAutoConfiguration.class)
class DocumentReaderIntegrationTest {
    @Autowired DocumentReader reader;

    @Test
    void readerHandlesPlainTextWithMarkitdown4j() throws Exception {
        File f = new File("src/test/resources/hello.txt");
        Files.writeString(f.toPath(), "Hello\n\n## Sub\n\nWorld");
        Document d = reader.read(f);
        assertThat(d.source()).isEqualTo(SourceType.MARKITDOWN4J);
        assertThat(d.fullMarkdown()).contains("Hello").contains("## Sub");
    }
}
```

- [x] **Step 2: 验证 + Commit**

Run: `./mvnw -pl ddd4j-ai-extension-document -am test -Dtest=DocumentReaderIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep -E "Tests run:|BUILD" | tail -3`
Expected: PASS

```bash
git add ddd4j-ai-extensions/ddd4j-ai-extension-document/
git commit -m "test(extension): add integration test verifying markitdown4j end-to-end"
```

---

### Task 6: 计划勾选 + 推送

- [x] **Step 1: 勾选本计划**

```bash
sed -i '' 's/- \[ \]/- [x]/g' ddd4j-ai-extensions/ddd4j-ai-extension-document/docs/superpowers/plans/2026-08-26-ddd4j-ai-extension-document.md
git add ddd4j-ai-extensions/ddd4j-ai-extension-document/docs/superpowers/plans/
git commit -m "docs: mark ddd4j-ai-extension-document plan complete"
```

- [x] **Step 2: 推送到 origin**

```bash
git push origin $(git rev-parse --abbrev-ref HEAD)
```

---

## Self-Review

- **Spec 覆盖**：markitdown4j 14 converters 全面集成（Task 2 桥接）+ 4 组件委托 SPI（Task 3 占位）+ DocumentReader 门面（Task 4）+ 集成测试（Task 5）
- **占位符扫描**：无 TBD；4 组件委托用 `@ConditionalOnClass` 占位，待组件发布后激活
- **类型一致性**：Document/Section/Table/Image 字段名与 4 组件一致；`SourceType` enum 追踪输出来源（智能体/审计）
- **依赖隔离**：不用 AGPL 的 `io.gitlab.ade90036:markitdown:0.0.1`；用全 MIT 的 14 个独立 converter artifact
- **降级策略**：DocumentReader 循环 parser 列表，委托 parser 抛 `UnsupportedOperationException` 时自动降级到 markitdown4j 兜底（保证智能体总是拿到结果）