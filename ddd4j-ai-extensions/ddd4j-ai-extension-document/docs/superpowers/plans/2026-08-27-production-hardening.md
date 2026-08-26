# ddd4j-ai-extension-document 生产加固计划（流式 / 限额超时 / 防护 / 可观测）

> **实施记录（2026-08-27，全部 6 Task 完成，document 模块 68 测试全绿）**：
> - 流式+限额（Task 1）：`TikaInputStream.get(Path)` 流式 spool；File 入口 `Files.size` 前置拒绝，Stream 入口 `LimitedInputStream` 流式计数拒绝；默认 100MB
> - 超时（Task 2）：`TikaTaskTimeout` 注入 ParseContext（默认 60s），超时不降级重试直接上抛
> - 防护（Task 3）：`SecureContentHandler` 包裹 handler 链（SAX 实体/输出量限额）+ 嵌入图数量（默认 20）/单图大小（默认 5MB）双限额，丢弃计数 → `metadata.embeddedImagesTruncated`
> - 可观测（Task 4）：SLF4J——INFO 每次解析（file/mime/source/sections/tables/images/tookMs），WARN 四类事件（OCR 降级/转写失败/超限拒绝/图片截断）
> - 对抗样本（Task 5）：空文件（短路返回空 Document + `metadata.empty=true`，比 ZeroByteFileException 更智能体友好）/截断 PDF/损坏 zip/15 层嵌套/110MB 压缩炸弹/空表格 HTML，全部 60s 超时断言内受控返回

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 修复「生产就绪评估」识别的 4 项硬缺口，使 document 模块达到 v1 生产标准：
1. **内存风险**：`parse()` 全量 `readAllBytes()` 入内存 → 流式 spool + 文件大小上限
2. **无超时**：病态文档可挂住解析线程 → `TikaTaskTimeout` 注入 ParseContext
3. **无防护**：zip 炸弹/深嵌套攻击面 → `SecureContentHandler` 包裹 + 嵌入资源数量/大小限额
4. **零可观测**：降级事件/解析耗时/来源不可见 → SLF4J 结构化日志

**关键 API（已核实于 tika-core 3.3.2 本地 jar，2026-08-27）**：
- `org.apache.tika.io.TikaInputStream.get(Path/InputStream, TemporaryResources, Metadata)` —— 流式（超阈值自动 spool 到磁盘临时文件）
- `org.apache.tika.config.TikaTaskTimeout(long timeoutMillis)` + `TikaTaskTimeout.getTimeoutMillis(ParseContext, default)` —— 解析超时（ParseContext 注入）
- `org.apache.tika.sax.SecureContentHandler(ContentHandler, TikaInputStream)` —— SAX 层防护（`setMaximumCompressionRatio`/`setOutputThreshold`，实体数与输出量限额）
- SLF4J 已在依赖树（Spring Boot 自动提供），零新增依赖

**Architecture（改动集中在 4 个文件 + 配置）**：
- `TikaDocumentParser`：`doParse` 改为 `TikaInputStream` 流式 + `SecureContentHandler` 包裹 `TeeContentHandler` + ParseContext 注入 `TikaTaskTimeout`；`parse(File/InputStream)` 入口先做大小上限检查（File 用 `Files.size`，InputStream 用 available 不可靠 → 读入时经 `BoundedInputStream` 限流并在超限时抛 `DocumentTooLargeException`）
- 嵌入资源：`EmbeddedImageExtractor` 加数量上限（`maxEmbeddedImages`，默认 20）与单图大小上限（`maxEmbeddedImageBytes`，默认 5MB）
- 音频转写：音频文件仍需 byte[]（asr 端口契约），受同一 `maxFileSizeBytes` 上限保护（音频文件通常 <100MB，可接受）
- 日志：`TikaDocumentParser` 加 SLF4J logger——每次解析一条 INFO（文件名/检测 MIME/来源/耗时/sections/tables/images 数）、每次降级一条 WARN（原因：OCR 不可用/转写失败/超限拒绝）
- 新增异常：`DocumentTooLargeException extends IllegalArgumentException`（明确业务语义，便于上层区分「文件太大」与「解析失败」）

**Tech Stack:** 现有依赖零新增（Tika core 已含全部防护类；SLF4J 由 Spring Boot starter 传递）。

## Global Constraints

- **不改**既有公共契约：`Document`/`DocumentParser`/`DocumentReader`/`SourceType`/`MediaType` 签名不动（新增异常类为新增，不影响既有调用方）
- 全部新行为有配置开关且默认值**生产安全**（默认即启用限额/超时；关掉需显式配置）
- 测试不依赖真实 Docker/外网/tesseract；恶意样本用代码构造（深嵌套 zip 用循环 ZipOutputStream、大文件用 `Arrays.repeat` 风格生成的稀疏内容或截断断言）
- 既有 46 测试必须保持全绿（默认限额不得误伤既有测试样本——均为 KB 级）
- 提交规范：conventional commits，每 Task 一提交；最终推送双远程（github + origin feature/2.0.x）
- 验证命令：`./mvnw -U -Denforcer.skip=true -B -DskipTests=false -pl ddd4j-ai-extensions/ddd4j-ai-extension-document -am test -Dsurefire.failIfNoSpecifiedTests=false` BUILD SUCCESS

---

### Task 1: 流式解析 + 文件大小上限（消灭 OOM 风险）

**Files:**
- Create: `src/main/java/io/ddd4j/ai/cmpt/document/DocumentTooLargeException.java`
- Modify: `src/main/java/io/ddd4j/ai/cmpt/document/parser/TikaDocumentParser.java`
- Modify: `src/main/java/io/ddd4j/ai/cmpt/document/properties/DocumentProperties.java`
- Test: `src/test/java/io/ddd4j/ai/cmpt/document/parser/SizeLimitTest.java`

**Interfaces:**
- `DocumentProperties` 新增：
  - `long maxFileSizeBytes = 104_857_600L`（100MB，`ddd4j.ai.document.max-file-size-bytes`；`<=0` 表示不限制）
- `DocumentTooLargeException(String message)` —— 上层可按类型捕获给出业务提示
- `parse(File)`：入口 `Files.size(file)` 超限直接抛（不打开文件）
- `parse(InputStream, filename)`：经 `org.apache.tika.io.BoundedInputStream`（若 core 无此公开类则自实现等价物：包裹读计数，超限抛 `DocumentTooLargeException`）复制到临时文件后走 `parse(File)`（保留现有临时文件模式，明确关闭流）
- `doParse(byte[])` 内部重构：仅音频转写分支保留 byte[]；通用解析改 `TikaInputStream.get(path/临时文件, tmp, metadata)` —— **不再对文档整体 readAllBytes**

- [x] **Step 1: 写失败测试** `SizeLimitTest`：
  - `fileExceedsLimit_throwsDocumentTooLarge`：properties.maxFileSizeBytes=10；写 11 字节 txt → 抛 `DocumentTooLargeException`
  - `streamExceedsLimit_throwsDocumentTooLarge`：同限额，ByteArrayInputStream 20 字节 → 抛 `DocumentTooLargeException`
  - `limitDisabled_acceptsAnySize`：maxFileSizeBytes=0；正常 txt 解析成功（回归）
  - `normalSizedFile_unchangedBehavior`：默认限额 + 普通 txt → 既有行为（内容/来源断言）
- [x] **Step 2: 运行确认失败**（异常类/限额不存在）
- [x] **Step 3: 实现**：异常类 + properties 字段 + File/Stream 两入口限额 + TikaInputStream 流式 `doParse`
- [x] **Step 4: 全模块测试通过（46+4）+ Commit**
  - `git commit -m "feat(document): streaming parse via TikaInputStream with configurable file size limit"`

---

### Task 2: 解析超时（TikaTaskTimeout）

**Files:**
- Modify: `src/main/java/io/ddd4j/ai/cmpt/document/parser/TikaDocumentParser.java`
- Modify: `src/main/java/io/ddd4j/ai/cmpt/document/properties/DocumentProperties.java`
- Test: `src/test/java/io/ddd4j/ai/cmpt/document/parser/TimeoutTest.java`

**Interfaces:**
- `DocumentProperties` 新增：`long parseTimeoutMillis = 60_000`（`ddd4j.ai.document.parse-timeout-millis`；`<=0` 不限时）
- `doParse`：`parseContext.set(TikaTaskTimeout.class, new TikaTaskTimeout(properties.getParseTimeoutMillis()))`
- 超时触发时 Tika 抛 `TikaTimeoutException` → 不吞、不降级重试（超时说明文档病态，重试只会再挂一次），直接上抛并在外层记 WARN 日志（Task 4 接入）

- [x] **Step 1: 写测试** `TimeoutTest`：
  - `timeoutConfigured_contextInjected`：properties.parseTimeoutMillis=5000 → 解析成功（正常小文档不受影响，回归语义）
  - `timeoutDisabled_defaultStillWorks`：parseTimeoutMillis=0 → 解析成功
  - `timeoutProperty_bound`：默认值断言 60_000（防误改默认）
  - （真实超时用病态样本触发不稳定，不做强制断言；靠上下文注入语义 + 默认值锁定）
- [x] **Step 2: 运行确认失败**（properties 无该字段）
- [x] **Step 3: 实现** + Step 4: 通过 + Commit
  - `git commit -m "feat(document): parse timeout via TikaTaskTimeout in ParseContext"`

---

### Task 3: SAX 防护 + 嵌入资源限额（zip 炸弹/深嵌套/图片轰炸）

**Files:**
- Modify: `src/main/java/io/ddd4j/ai/cmpt/document/parser/TikaDocumentParser.java`
- Modify: `src/main/java/io/ddd4j/ai/cmpt/document/properties/DocumentProperties.java`
- Test: `src/test/java/io/ddd4j/ai/cmpt/document/parser/SecurityLimitsTest.java`

**Interfaces:**
- `doParse` 的 handler 链改为：`SecureContentHandler(TeeContentHandler(ToMarkdown, Structure), tikaStream)`（替代裸 Tee）
- `DocumentProperties` 新增：
  - `int maxEmbeddedImages = 20`（超出丢弃并在 metadata 记 `embeddedImagesTruncated=true`）
  - `long maxEmbeddedImageBytes = 5_242_880`（5MB/图，超图丢弃）
- `EmbeddedImageExtractor` 构造接收两限额，达到数量上限后 `shouldParseEmbedded` 返回 false
- 大小上限超限的嵌入图：跳过收集、计数 `truncatedImages`（metadata 透传）

- [x] **Step 1: 写测试** `SecurityLimitsTest`：
  - `deeplyNestedZip_parsesOrFailsGracefully`：构造 10 层嵌套 zip（每层含下一层）→ 不挂死、60s 内返回（解析成功或受控异常均可，断言「方法在期限内返回」）
  - `embeddedImages_cappedAtLimit`：构造含 3 图的 docx，maxEmbeddedImages=2 → images 含 ≤2 个 data URL 图且 `metadata.embeddedImagesTruncated=true`
  - `oversizedEmbeddedImage_skipped`：单图 6MB（maxEmbeddedImageBytes=5MB）→ 该图跳过、解析成功、metadata 记 truncated
- [x] **Step 2: 运行确认失败**
- [x] **Step 3: 实现**（SecureContentHandler 包裹 + extractor 限额）
- [x] **Step 4: 通过 + Commit**
  - `git commit -m "feat(document): SecureContentHandler + embedded resource caps (zip bomb / image flood protection)"`

---

### Task 4: 可观测性（SLF4J 结构化日志）

**Files:**
- Modify: `src/main/java/io/ddd4j/ai/cmpt/document/parser/TikaDocumentParser.java`
- Test: `src/test/java/io/ddd4j/ai/cmpt/document/parser/LoggingTest.java`

**Interfaces:**
- `private static final Logger log = LoggerFactory.getLogger(TikaDocumentParser.class)`
- INFO 一条/次解析（成功路径，结束处）：`document parsed: file={}, mime={}, source={}, sections={}, tables={}, images={}, tookMs={}`
- WARN 降级与拒绝事件：
  - OCR 不可用降级：`ocr unavailable, falling back to plain parse: {}`
  - 转写失败：`asr transcription failed, metadata only: {}`
  - 超限：`document rejected, exceeds size limit: file={}, size={}, limit={}`
  - 嵌入图截断：`embedded images truncated: kept={}, dropped={}`
- 测试用 `ListAppender`（logback 经 spring-boot-starter-test 在 test classpath）捕获断言 INFO/WARN 内容

- [x] **Step 1: 写测试** `LoggingTest`：
  - `successfulParse_logsInfoWithMetrics`：解析 txt → 捕获 1 条 INFO 含 `tookMs` 与 `mime=text/plain`
  - `sizeRejection_logsWarn`：超限 → WARN 含 `exceeds size limit`
  - `truncation_logsWarn`：嵌入图截断 → WARN 含 `truncated`
- [x] **Step 2: 确认失败**（无日志）→ **Step 3: 实现埋点** → **Step 4: 通过 + Commit**
  - `git commit -m "feat(document): structured SLF4J observability (parse metrics + degradation events)"`

---

### Task 5: 恶意/边界样本回归（真实攻击面）

**Files:**
- Create: `src/test/java/io/ddd4j/ai/cmpt/document/AdversarialSamplesTest.java`

**Interfaces:** 每类攻击面一个测试，断言「受控结果」（明确异常或降级成功），绝不挂死/静默吞：
| 样本 | 构造 | 期望 |
|------|------|------|
| 空文件 | 0 字节 | 解析成功（空内容）或 ZeroByteFile 受控异常 |
| 截断 PDF | 合法 PDF 前缀 + 截断 | 受控异常（不挂死） |
| 损坏 zip（假 docx） | zip 魔数 + 乱码 | 受控异常或空解析 |
| 深嵌套 zip | 15 层互嵌 | 期限内返回 |
| 高压缩比 zip 炸弹 | 小 zip 解压后 100MB 重复字节（受 maxFileSize 保护） | DocumentTooLargeException 或受控 |
| 空表格 HTML | `<table><tr></tr></table>` | sections/tables 空集合不 NPE |

- [x] **Step 1: 写 6 个样本测试**（对现有实现跑，暴露问题）
- [x] **Step 2: 修正实现直至全绿**（预期前 5 项可能暴露需修的点，第 6 项回归）
- [x] **Step 3: Commit**
  - `git commit -m "test(document): adversarial samples regression (empty/truncated/corrupt/nested/bomb)"`

---

### Task 6: 文档回写 + 全量验证 + 发布推送

- [x] **Step 1: 计划勾选**（sed 全勾）+ 在本计划头部追加实施记录（含默认限额表）
- [x] **Step 2: README/架构 spec 更新**：document 行补「生产加固：流式/限额/超时/防护/可观测」
- [x] **Step 3: 全量 reactor 验证**
  - `./mvnw -U -Denforcer.skip=true -B -DskipTests=false clean test` 23 模块 BUILD SUCCESS
- [x] **Step 4: deploy document 模块到私仓 + 推送双远程**
  - `./mvnw -pl ddd4j-ai-extensions/ddd4j-ai-extension-document -am deploy`（snapshot 通道已打通）
  - `git push github feature/2.0.x && git push origin feature/2.0.x`

---

## Self-Review

- **覆盖评估缺口**：生产就绪评估的 1（内存/Task 1）、2（超时/Task 2）、3（防护/Task 3）、4（可观测/Task 4）、5（真实样本→恶意样本回归/Task 5）全部对应；第 6 项（release 版本 pin）属版本管理流程非代码，不在本计划
- **默认即安全**：限额/超时默认开启且值保守（100MB/60s/20 图/5MB 图）；关闭需显式配置
- **向后兼容**：公共契约零改动；46 既有测试必须全绿（KB 级样本不受默认限额影响）
- **测试独立性**：全部样本代码构造（zip 嵌套/炸弹用 ZipOutputStream 循环），无 Docker/外网/tesseract
- **占位扫描**：无 TODO；超时的真实触发不做不稳定断言（用注入语义 + 默认值锁定替代），理由已注明
