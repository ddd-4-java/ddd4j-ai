# OCR 文档/图像识别组件设计

- 日期：2026-08-12
- 作者：PartMe.AI
- 状态：待实施（骨架：仅 package-info 占位）
- 范围：`ddd4j-ai-extension-ocr` —— 文档/图像文字识别
- 关联文档：整体架构见 `2026-08-07-ddd4j-ai-architecture-design.md`；可接入 rag 作为文档摄取前置

---

## 1. 背景

OCR 是知识库（RAG）摄取非结构化文档（PDF、Office、图像）的前置能力。ocr 组件规划对接 Apache PDFBox（PDF 文本提取）、Apache Tika（多格式文档解析），并预留图像 OCR 扩展（Azure Vision / Tesseract）。

## 2. 目标

- 提供 PDF 文本提取（PDFBox）。
- 提供多格式文档解析（Tika：doc/docx/xls/xlsx 等）。
- 预留图像 OCR 扩展点。
- 输出统一 `Document` 结构，便于接入 rag 摄取管道。

### 非目标

- 不实现向量嵌入/检索（交 vectordb/rag）。
- 不内置图像模型训练。

## 3. 关键决策（建议）

| # | 决策候选 | 理由 |
|---|---------|------|
| O1 | PDF 走 PDFBox、Office/其他走 Tika | 各取所长 |
| O2 | 端口接口统一 `OcrService.parse(...)` → `Document` | 与 rag 解耦 |
| O3 | 图像 OCR 以扩展点暴露，默认不绑定模型 | 按需引入 Azure Vision/Tesseract |

## 4. 总体架构

```
rag/业务 → OcrService（端口）→ impl/{PdfBox, Tika, Image}Parser → Document
```

## 5. 接口方向

- `OcrService`：`parse(InputStream, format)` → `List<Document>`（支持分页/分块）。

## 6. 模块落点

```
ddd4j-ai-extension-ocr/src/main/java/io/ddd4j/ai/cmpt/ocr
├── properties/ (OcrProperties: 图像 OCR 提供方)
├── service/OcrService.java
└── service/impl/{PdfBox, Tika, Image}Parser.java
```

## 7. 依赖

> ⚠️ **待引入依赖**：README 历史版本表列出 `Apache PDFBox 3.0.5` 与 `Apache Tika BOM 3.2.2`，但 `ddd4j-ai-dependencies/pom.xml` **当前未直接声明**。实施前需确认：
> - 是否经 `ddd4j-boot-dependencies` 间接提供；
> - 若未提供，需在本组件 pom 或 `ddd4j-ai-dependencies` 补声明版本。

实施 Task 第一步即为依赖落地，详见 `2026-08-12-ddd4j-ai-v1x-rag-agent-mcp-ocr.md`。

## 8. 测试策略

- 各 Parser 对样例文档解析单测；大 PDF 分页/分块正确性；格式自动识别测试。
