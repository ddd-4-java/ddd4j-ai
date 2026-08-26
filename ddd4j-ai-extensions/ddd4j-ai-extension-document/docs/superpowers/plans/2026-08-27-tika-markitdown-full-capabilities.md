# ddd4j-ai-extension-document v2：Tika 底座对齐 markitdown 全部能力

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 以 **Apache Tika 3.3.2 为唯一技术底座**，在现有 `ddd4j-ai-extension-document` 模块（`Document` POJO + `DocumentParser` SPI + `DocumentReader` 门面 + `TikaDocumentParser` 已具备：结构化 Markdown / 章节树 / 表格 / 图片 / MIME 嗅探）之上，补齐 **markitdown（微软）的全部能力**：OCR、图片 EXIF、音频元数据、音频语音转写、嵌入资源提取、语言检测。单一引擎、Java 17、零新增第三方转换器。

**关键定位（用户已确认）**：
- Tika 底座 **不是过渡方案**，而是最终实现：markitdown 的每一项能力都有 Tika 原生对应物（下表核实于 2026-08-27，全部类已在 `tika-parsers-standard-package` 依赖内，零新增）
- markitdown 特有的**在线资源**（YouTube 字幕 / 维基百科）属网络抓取，Tika 无 → 声明为边界外（`DocumentParser` SPI 已允许未来以独立 parser 扩展，本批不做）
- 唯一需要跨组件联动的是**音频语音转写**：Tika 只给音频元数据，转写委托 `ddd4j-ai-extension-asr`（WhisperCpp，可选注入）

**Architecture:**
- 全部增强集中在 `TikaDocumentParser`（单一入口，保持 SPI/门面/POJO 不变）：
  - 解析管线：`AutoDetectParser` + `TeeContentHandler(ToMarkdownContentHandler ‖ MarkdownStructureHandler)`（基线已有）→ 本批增加：
    - `ParseContext` 注入 `TesseractOCRConfig`（OCR 开关）
    - `ParseContext` 注入 `EmbeddedDocumentExtractor`（嵌入图片 → base64 data URL）
    - `Metadata` 全量透传 → `Document.metadata`（EXIF/音频/通用）
    - `LanguageDetector` 语言检测 → `metadata.language`
    - 音频分支：`AsrService`（可选注入）转写 → 文本并入 sections
- 全部能力默认**关闭或优雅降级**（OCR 无 tesseract → 普通文本；无 AsrService → 仅元数据），不破坏现有 27 测试

**Tech Stack:** Apache Tika 3.3.2（tika-core + tika-parsers-standard-package，已在依赖治理）、Spring AI Commons 2.0.0、`ddd4j-ai-extension-asr`（WhisperCpp 转写委托）、Spring Boot AutoConfigure。

## markitdown 能力 ↔ Tika 底座映射（事实核对 2026-08-27）

| markitdown 能力 | Tika 底座实现 | 归属类（已核实存在） | 状态 |
|-----------------|--------------|---------------------|------|
| PDF / DOCX / PPTX / XLSX / CSV / HTML / EPUB / ZIP | AutoDetectParser 全格式 | tika-parsers-standard-package | ✅ 基线已有 |
| 干净 GFM Markdown（标题/列表/表格/链接） | ToMarkdownContentHandler | `org.apache.tika.sax.ToMarkdownContentHandler` | ✅ 基线已有 |
| 章节层级结构 | MarkdownStructureHandler（SAX h1-h6 → Section 树） | 自研（已实现） | ✅ 基线已有 |
| 表格结构化 | MarkdownStructureHandler（table → DocumentTable） | 自研（已实现） | ✅ 基线已有 |
| 图片 alt/src | MarkdownStructureHandler（img → DocumentImage） | 自研（已实现） | ✅ 基线已有 |
| MIME 检测（内容嗅探） | Tika.detect(byte[], name) | `org.apache.tika.Tika` | ✅ 基线已有 |
| **图片 OCR / 扫描 PDF** | TesseractOCRConfig + TesseractOCRParser | `org.apache.tika.parser.ocr.TesseractOCRConfig` | ⬜ Task 1 |
| **图片 EXIF 元数据** | ImageParser 元数据透传 | `org.apache.tika.parser.image.ImageParser` | ⬜ Task 2 |
| **音频元数据**（mp3/flac 等） | AudioParser 元数据透传 | `org.apache.tika.parser.audio.AudioParser` | ⬜ Task 2 |
| **语言检测** | LanguageDetector | `org.apache.tika.language.detect.LanguageDetector` | ⬜ Task 2 |
| **音频语音转写** | 委托 `ddd4j-ai-extension-asr`（WhisperCpp） | `AsrService`（可选注入） | ⬜ Task 3 |
| **嵌入资源提取**（图片/附件） | EmbeddedDocumentExtractor → base64 data URL | `org.apache.tika.extractor.EmbeddedDocumentExtractor` | ⬜ Task 4 |
| DOCX 公式 → LaTeX | Tika 输出 OMML 文本/MathML（有限） | — | ⚠️ 边界：尽力而为 |
| YouTube / 维基百科（在线） | Tika 无网络抓取 | — | 🚫 边界：不做（SPI 可扩展） |

## Global Constraints

- **不改** `Document`/`DocumentSection`/`DocumentTable`/`DocumentImage`/`MediaType`/`SourceType`/`DocumentParser`/`DocumentReader`（已稳定，27 测试基线）
- 新增能力默认关闭或降级：`ddd4j.ai.document.ocr-enabled`（默认 false）、`enable-language-detection`（默认 true）、`asr-service` 可选注入
- 全部测试**无需 Docker / 外网 / 宿主机 tesseract**（OCR 探测失败优雅降级，断言不依赖真实 OCR）
- 依赖零新增（OCR/音频/图像/语言检测/嵌入提取全在现有 standard-package 内）；仅 document pom 增加 `ddd4j-ai-extension-asr` 依赖（转写委托）
- 提交规范：conventional commits；每 Task 独立提交；最终推送双远程（github + origin 的 feature/2.0.x）
- 验证命令：`./mvnw -U -Denforcer.skip=true -B -DskipTests=false -pl ddd4j-ai-extensions/ddd4j-ai-extension-document -am test -Dsurefire.failIfNoSpecifiedTests=false` 必须 BUILD SUCCESS

---

### Task 1: OCR 支持（扫描 PDF / 图片文字识别）

**Files:**
- Modify: `src/main/java/io/ddd4j/ai/cmpt/document/parser/TikaDocumentParser.java`
- Modify: `src/main/java/io/ddd4j/ai/cmpt/document/properties/DocumentProperties.java`
- Test: `src/test/java/io/ddd4j/ai/cmpt/document/parser/TikaOcrEnabledTest.java`

**Interfaces:**
- `DocumentProperties` 增加：
  - `boolean ocrEnabled = false`（`ddd4j.ai.document.ocr-enabled`）
  - `String ocrLanguage = "eng"`
- `TikaDocumentParser` 增加构造参数 `DocumentProperties`（OCR 开关）；`parse(byte[])` 内当 `ocrEnabled` 时：
  ```java
  TesseractOCRConfig config = new TesseractOCRConfig();
  config.setLanguage(properties.getOcrLanguage());
  config.setOutputType(TesseractOCRConfig.OUTPUT_TYPE.TXT);
  parseContext.set(TesseractOCRConfig.class, config);
  parseContext.set(TesseractOCRParser.class, new TesseractOCRParser());
  ```
- **降级**：OCR 开启但宿主机无 tesseract 可执行文件时，`TesseractOCRParser` 解析抛异常 → catch 后按无 OCR 路径重解析（`ParseContext` 不注入 OCR），保证智能体总能拿到结果（对齐 markitdown 的"OCR 失败不阻塞"）

- [ ] **Step 1: 写失败测试** `TikaOcrEnabledTest`：
  - `ocrEnabled_true_withoutTesseract_fallsBackToPlainText`：properties.ocrEnabled=true，解析纯文本 txt → 仍返回文本（不抛异常，source=TIKA_FALLBACK）
  - `ocrEnabled_false_default`：默认构造不注入 OCR（行为与基线一致）
  - `ocrDisabled_parsesImageMetadataOnly`：PNG 字节（1x1）→ 解析成功（元数据，无 OCR 依赖）
- [ ] **Step 2: 运行确认失败**（TikaDocumentParser 尚无 properties 构造）
- [ ] **Step 3: 实现**：DocumentProperties 加字段；TikaDocumentParser 加 `DocumentProperties` 构造（保留无参构造委托默认值，避免破坏 AutoConfiguration 之外用法）+ OCR 注入 + 降级重解析
- [ ] **Step 4: 验证通过 + Commit**
  - 验证命令跑 document 全量测试（含基线 27）
  - `git commit -m "feat(document): OCR support via TesseractOCRConfig with graceful fallback"`

---

### Task 2: 元数据透传 + 语言检测

**Files:**
- Modify: `src/main/java/io/ddd4j/ai/cmpt/document/parser/TikaDocumentParser.java`
- Modify: `src/main/java/io/ddd4j/ai/cmpt/document/properties/DocumentProperties.java`
- Test: `src/test/java/io/ddd4j/ai/cmpt/document/parser/TikaMetadataTest.java`

**Interfaces:**
- `parse(byte[])` 内把 Tika `Metadata`（解析时收集）全量并入 `Document.metadata`，并精选键做规范化：
  - `author` ← `dc:creator` / `Author`
  - `created` ← `dcterms:created` / `Creation-Date`
  - `pageCount` ← `xmpTPg:NPages`
  - `contentType` ← `Content-Type`（与 detectedMime 一致时省略）
  - `language` ← `LanguageDetector`（`new LanguageDetector()` 或 `TikaConfig.getDefaultConfig().getLanguageDetector()`）检测文本（用 sections 拼接的 markdown 前 N 字符）
- `DocumentProperties` 增加 `boolean enableLanguageDetection = true`
- 注意：`Metadata` 键含 `Content-Type`（Tika 检测结果）会与 `detectedMime` 重复 → 统一保留 `detectedMime`，Tika 原生键去重

- [ ] **Step 1: 写失败测试** `TikaMetadataTest`：
  - `metadata_carriesTikaKeys`：HTML 解析 → `metadata` 含 `Content-Type`（或 `detectedMime`）且含 `source`
  - `language_detectedForChineseText`：中文 txt → `metadata.language` 非空（zh 系）
  - `language_detectedForEnglishText`：英文 txt → `metadata.language` 含 "en"
- [ ] **Step 2: 运行确认失败**（metadata 尚未透传/无 language）
- [ ] **Step 3: 实现**：`parse(byte[])` 返回 `Parsed(markdown, sections, tables, images, tikaMetadata)`；`map` 合并元数据 + LanguageDetector
- [ ] **Step 4: 验证通过 + Commit**
  - `git commit -m "feat(document): expose Tika metadata (EXIF/audio/author/pages) and language detection"`

---

### Task 3: 音频语音转写（委托 asr 组件，对齐 markitdown 音频转写）

**Files:**
- Modify: `ddd4j-ai-extension-document/pom.xml`（+ `ddd4j-ai-extension-asr` 依赖）
- Modify: `src/main/java/io/ddd4j/ai/cmpt/document/parser/TikaDocumentParser.java`
- Modify: `src/main/java/io/ddd4j/ai/cmpt/document/autoconfigure/DocumentAutoConfiguration.java`
- Test: `src/test/java/io/ddd4j/ai/cmpt/document/parser/TikaAudioTranscriptionTest.java`

**Interfaces:**
- `TikaDocumentParser` 增加可选 `AsrService`（`io.ddd4j.ai.cmpt.asr.service.AsrService`）构造参数（可 null）
- 音频判定：`TIKA.detect(bytes, name)` 结果以 `audio/` 开头 或 MediaType 为音频扩展（wav/mp3/flac/ogg/m4a）时：
  - 先走 Tika `AudioParser` 元数据（Task 2 已透传）
  - 若 `asrService != null`：`asrService.transcribe(bytes, AudioFormat.wav44100Stereo16())` → 转写文本追加为 `DocumentSection("Transcription", 1, text)`（markdown 全文追加段落）
  - 无 asrService 或转写抛异常 → 仅保留元数据（优雅降级）
- `DocumentAutoConfiguration`：`tikaDocumentParser(DocumentProperties, ObjectProvider<AsrService>)` 可选注入
- **依赖**：document pom 增加 `ddd4j-ai-extension-asr`（compile）；asr 组件 JNA 原生缺失不影响（探测降级已有）

- [ ] **Step 1: 写失败测试** `TikaAudioTranscriptionTest`（mock AsrService，不依赖 native）：
  - `audio_withAsrService_appendsTranscription`：WAV 字节（16k mono 简谐波）→ mock asrService 返回 "spoken text" → Document.sections 含 Transcription section、fullMarkdown 含 "spoken text"
  - `audio_withoutAsrService_metadataOnly`：无 asrService → 解析成功、无 Transcription section
  - `audio_asrFailure_fallsBackToMetadataOnly`：mock 抛异常 → 不中断、无 Transcription
- [ ] **Step 2: 运行确认失败**（构造/接口未就绪）
- [ ] **Step 3: 实现**：pom 依赖 + TikaDocumentParser 音频分支 + AutoConfiguration 注入
- [ ] **Step 4: 验证通过 + Commit**
  - `git commit -m "feat(document): audio transcription delegated to ASR component (markitdown parity)"`

---

### Task 4: 嵌入资源提取（图片 → base64 data URL）

**Files:**
- Modify: `src/main/java/io/ddd4j/ai/cmpt/document/parser/TikaDocumentParser.java`
- Test: `src/test/java/io/ddd4j/ai/cmpt/document/parser/TikaEmbeddedResourceTest.java`

**Interfaces:**
- `parse(byte[])` 内 `ParseContext` 注入自研 `EmbeddedDocumentExtractor` 实现：
  ```java
  parseContext.set(EmbeddedDocumentExtractor.class, new EmbeddedDocumentExtractor() {
      @Override public boolean shouldParseEmbedded(Metadata metadata) { return true; }
      @Override public void parseEmbedded(InputStream stream, ContentHandler handler, Metadata metadata, boolean outputHtml) {
          // 收集 image/* 嵌入资源 → images 列表（src = "data:<mime>;base64,<...>"）
      }
  });
  ```
- 仅收集 `image/*`（对齐 markitdown 的图片提取）；`alt` 取 `metadata` 的 `resourceName` 或空
- base64：`java.util.Base64`；`src` 格式 `data:image/png;base64,iVBOR...`（DocumentImage.src 语义已支持）
- 降级：非容器格式（txt/csv）无嵌入资源 → images 保持空

- [ ] **Step 1: 写失败测试** `TikaEmbeddedResourceTest`：
  - `docxWithEmbeddedImage_collectsBase64Image`：构造最小 docx（OOXML zip：`[Content_Types].xml` + word/document.xml 含 `<w:drawing>...<a:blip r:embed="rId1"/>` + word/_rels/document.xml.rels 指 rId1 → media/image1.png）+ 1x1 PNG 字节 → Document.images 非空，src 以 `data:image/png;base64,` 开头
  - `plainText_noEmbeddedResources`：txt → images 空
- [ ] **Step 2: 运行确认失败**
- [ ] **Step 3: 实现**：内嵌 `EmbeddedImageExtractor implements EmbeddedDocumentExtractor`（收集 images）
- [ ] **Step 4: 验证通过 + Commit**
  - `git commit -m "feat(document): extract embedded images as base64 data URLs (markitdown parity)"`

---

### Task 5: 格式能力矩阵端到端测试

**Files:**
- Modify: `src/test/java/io/ddd4j/ai/cmpt/document/DocumentReaderIntegrationTest.java`
- Create: `src/test/java/io/ddd4j/ai/cmpt/document/FormatMatrixIntegrationTest.java`

**Interfaces:**
- 用可本地构造的样本验证能力矩阵（无 Docker/外网）：
  | 格式 | 样本构造 | 断言 |
  |------|---------|------|
  | txt | 文本 | markdown 内容 / mime text/plain |
  | html | 结构化 html（h1/table/img） | sections 树 / tables / images / `# Title` |
  | csv | csv 文本 | markdown 含表头 |
  | png | 1x1 PNG 字节 | 解析成功 + metadata 非空（EXIF 路径） |
  | wav | 16k mono PCM WAV | 音频分支（无 asr → 元数据；mock asr → 转写） |
  | pdf | PDFBox 生成（已有） | 文本 + mime application/pdf |
  | docx | 最小 OOXML zip（Task 4 复用） | 文本 + 嵌入图片 |
- `FormatMatrixIntegrationTest`：每格式一个 `@Test`，走 `DocumentReader`（真实 AutoConfiguration 上下文）

- [ ] **Step 1: 写矩阵测试**（复用 Task 1-4 的样本构造 helper）
- [ ] **Step 2: 运行确认**
- [ ] **Step 3: 修正断言直至全绿**（Tika 对个别格式输出差异在此校正）
- [ ] **Step 4: Commit**
  - `git commit -m "test(document): format capability matrix end-to-end (txt/html/csv/png/wav/pdf/docx)"`

---

### Task 6: 文档回写 + 全量验证 + 推送

- [ ] **Step 1: 更新模块 spec/README 能力矩阵**
  - 在 `docs/superpowers/plans/2026-08-26-ddd4j-ai-extension-document.md` 追加 v2 实施记录（markitdown 能力对齐表 + 边界声明）
  - README 组件表 document 行更新为「Tika 底座：markitdown 全能力（OCR/EXIF/音频转写/语言检测/嵌入资源）」
- [ ] **Step 2: 勾选本计划**（sed 全勾）
- [ ] **Step 3: 全量 reactor 验证**
  - `./mvnw -U -Denforcer.skip=true -B -DskipTests=false clean test`（23+ 模块）BUILD SUCCESS
- [ ] **Step 4: 推送双远程**
  - `git push github feature/2.0.x && git push origin feature/2.0.x`

---

## Self-Review

- **能力覆盖**：markitdown 15 项能力中 12 项在 Tika 底座实现（表内 ✅+⬜），1 项尽力而为（LaTeX 公式），2 项声明边界（YouTube/维基百科在线源）—— 边界项均有明确理由（Tika 无网络抓取）与扩展位（SPI）
- **零新增第三方**：OCR/图像/音频/语言/嵌入提取全部在现有 `tika-parsers-standard-package`；唯一新依赖是自家 asr 组件（转写委托）
- **降级完整性**：OCR 无 tesseract → 普通文本；音频无 asr → 仅元数据；嵌入提取失败 → 空列表 —— 智能体总能拿到结果（对齐 markitdown 的"OCR 失败不阻塞"哲学）
- **占位符扫描**：无 TODO/TBD；LaTeX 公式明确标注"尽力而为"而非承诺
- **测试独立性**：全部单测/矩阵测试无需 Docker、外网、宿主机 tesseract（OCR 用降级断言）
