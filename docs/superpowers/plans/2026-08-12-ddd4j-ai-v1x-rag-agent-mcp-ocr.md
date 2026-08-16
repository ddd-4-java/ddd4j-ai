# v1.x-B 高阶能力（mcp / ocr / rag / agent）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 MCP 工具协议、OCR 文档识别、RAG 检索增强、Agent 智能体编排四个高阶组件，覆盖典型 LLM 应用场景。

**Architecture:** RAG 编排 chat+embedding+vectordb（v1.x-A 产出）；Agent 编排 chat+memory+mcp；MCP 经 SDK 暴露/消费 Tool/Resource/Prompt；OCR 经 PDFBox+Tika 独立工作并可为 RAG 提供文档摄取。

**Tech Stack:** Java 17、Maven、Spring AI 2.0.0、MCP SDK 2.0.0、AgentScope Java 2.0.1、Apache PDFBox 3.0.5（待引入）、Apache Tika 3.2.2（待引入）。

**Related Design Doc:** `docs/superpowers/specs/2026-08-12-mcp-component-design.md`、`2026-08-12-ocr-component-design.md`、`2026-08-12-rag-component-design.md`、`2026-08-12-agent-component-design.md`；架构基线 `2026-08-07-ddd4j-ai-architecture-design.md`。

## Global Constraints

- 前置依赖：v1.x-A 的 chat/memory/embedding/vectordb 已就绪。
- ⚠️ **PDFBox/Tika 依赖未在 `ddd4j-ai-dependencies` 声明**（见架构 spec §9.3），OCR Task 第一步必须先落地依赖（确认 boot-dependencies 是否间接提供，否则补声明）。
- TDD；每组件独立模块；不侵入 core。
- 提交约定：conventional commits。

---

## Task 1: 实现 ddd4j-ai-extension-mcp

**Files:**
- Create: `.../cmpt/mcp/service/{McpClientService,McpServerService}.java`、`service/impl/...`、`autoconfigure/McpAutoConfiguration.java`、`properties/McpProperties.java`
- Test: `.../cmpt/mcp/service/McpClientServiceTest.java`（嵌入式 MCP server）

**Interfaces:** Produces McpClient/ServerService；Consumes MCP SDK 2.0.0

- [ ] **Step 1:** 写失败测试 —— `callTool(name,args)` 经嵌入式 server 返回；`listTools()` 枚举；`exposeTool(ToolSpec)` schema 生成。
- [ ] **Step 2:** 定义双向端口（client 消费 / server 暴露）。
- [ ] **Step 3:** 集成 MCP SDK；AutoConfiguration 装配。
- [ ] **Step 4:** 绿后 `git commit -m "feat(mcp): 实现 MCP 客户端/服务端组件"`。

---

## Task 2: 实现 ddd4j-ai-extension-ocr

**Files:**
- Modify: `ddd4j-ai-dependencies/pom.xml`（确认/补声明 PDFBox 3.0.5 + Tika 3.2.2）
- Create: `.../cmpt/ocr/service/OcrService.java`、`service/impl/{PdfBox,Tika,Image}Parser.java`、`properties/OcrProperties.java`
- Test: `.../cmpt/ocr/service/OcrServiceContractTest.java`、`impl/{PdfBox,Tika}ParserTest.java`

**Interfaces:** Produces OcrService；Consumes PDFBox / Tika

- [ ] **Step 1:** 先确认 PDFBox/Tika 依赖来源（boot-dependencies 间接？），若未提供则在 `ddd4j-ai-dependencies` 补声明版本，`git commit -m "chore(deps): 引入 PDFBox/Tika 版本管理"`。
- [ ] **Step 2:** 写失败测试 —— 样例 PDF → 文本（PdfBox）；docx → 文本（Tika）；格式自动识别。
- [ ] **Step 3:** 定义 OcrService 端口（`parse(InputStream, format)` → `List<Document>`）。
- [ ] **Step 4:** 实现 PdfBox/Tika/Image Parser；Image 预留扩展点。
- [ ] **Step 5:** 绿后 `git commit -m "feat(ocr): 实现文档识别组件（PDFBox+Tika）"`。

---

## Task 3: 实现 ddd4j-ai-extension-rag

> **Completed 2026-08-16**（v1.x-B 提前交付）：`RagService`（ingest/query/queryStream）+ `Reranker` 扩展点（默认 `NoopReranker` 直通）+ `RagPipeline`（组合 chat/vectordb 自家端口，PromptTemplate 模板可配，TokenTextSplitter 分块可配开关）。测试：管道 mock 测试 + 装配测试 + Ollama×pgvector 联合冒烟 `RagSmokeIntegrationTest`。

**Files:**
- Create: `.../cmpt/rag/service/RagService.java`、`service/impl/RagPipeline.java`、`properties/RagProperties.java`
- Test: `.../cmpt/rag/service/RagServiceIT.java`（mock 三组件）

**Interfaces:** Produces RagService；Consumes chat/embedding/vectordb（组合）

- [ ] **Step 1:** 写失败测试 —— ingest 入库；query 检索→拼装 prompt→生成；rerank 扩展点可替换。
- [ ] **Step 2:** 定义 RagService 端口（`ingest`/`query`/`queryStream`）。
- [ ] **Step 3:** 实现 RagPipeline：摄取管道（Reader→Splitter→Embedding→VectorDb）；查询管道（search→[rerank]→prompt→chat）。
- [ ] **Step 4:** RagProperties（topK/模板/rerank 开关）。
- [ ] **Step 5:** 绿后 `git commit -m "feat(rag): 实现检索增强生成管道"`。

---

## Task 4: 实现 ddd4j-ai-extension-agent

**Files:**
- Create: `.../cmpt/agent/dto/{AgentTask,AgentResult,AgentStep}.java`、`service/AgentService.java`、`service/impl/{ReAct,PlanExecute}Agent.java`、`properties/AgentProperties.java`
- Test: `.../cmpt/agent/service/AgentServiceTest.java`

**Interfaces:** Produces AgentService；Consumes chat/memory/mcp

- [ ] **Step 1:** 写失败测试 —— ReAct 单轮思考→工具→终止；Plan-Execute 多步；工具调用经 mcp 端口；循环终止条件。
- [ ] **Step 2:** 定义 AgentService 端口与编排策略接口。
- [ ] **Step 3:** 实现 ReAct/PlanExecute 策略；上下文走 memory。
- [ ] **Step 4:** 可选对接 AgentScope Java 2.0.1 运行时。
- [ ] **Step 5:** 绿后 `git commit -m "feat(agent): 实现智能体编排组件（ReAct/PlanExecute）"`。

---

## Task 5: 集成验证（rag + agent + mcp）

**Files:**
- Test: `ddd4j-ai-samples/src/test/.../HighOrderSmokeIT.java`

- [ ] **Step 1:** 写联合冒烟：OCR 摄取文档 → RAG 入库 → Agent 经 MCP 调工具回答。
- [ ] **Step 2:** 绿后 `git commit -m "test(samples): 高阶能力联合冒烟"`。

---

## Task 6: 补齐本批各组件 sample 示例

**Files:**
- Create: `ddd4j-ai-samples/.../samples/{mcp,ocr,rag,agent}/...`

- [ ] **Step 1:** 每组件最小可运行示例。
- [ ] **Step 2:** `git commit -m "docs(samples): 补充 mcp/ocr/rag/agent 示例"`。

---

## Self-Review 结论

- **Spec coverage:** Task 1–4 一一对应四份组件 spec；Task 2 Step 1 显式处理 OCR spec §7 的待引入依赖。✅
- **Placeholder scan:** 无 TODO/TBD。✅
- **Type consistency:** 端口/impl 命名与 v1.x-A 一致；AgentStep/Result DTO 与 RagService Document 对齐。✅
