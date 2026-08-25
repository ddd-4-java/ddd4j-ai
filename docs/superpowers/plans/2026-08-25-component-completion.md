# ddd4j-ai v1.x-B/v2.0 组件补全（mcp/ocr/agent/flow/router/asr/tts）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成 v1.x-B（mcp/ocr/agent/flow）与 v2.0（router/asr/tts）共 6 个组件的代码、测试、文档落地，全部对齐既有 ddd4j-ai 架构范式（端口 + AutoConfiguration + COLA 分层）。

**Architecture:** 每个组件遵循既有范式——`service/<Port>.java` 端口 + `service/impl/*Adapter.java` 实现 + `properties/*Properties.java` + `autoconfigure/*AutoConfiguration.java` + `META-INF/spring/...AutoConfiguration.imports`。复用 ddd4j-ai-dependencies 已治理的 Spring AI 2.0 / MCP 2.0 / Spring AI Alibaba 2.0.0-M1.1 / PDFBox / Tika / Edge TTS / WhisperCpp / Micronaut 不进入。

**Tech Stack:** Java 17、Maven 4.0.0-rc-6（wrapper）、Spring Boot 4.0.5（FW 7.0.8）、Spring AI 2.0、Spring AI Alibaba Graph 2.0.0-M1.1、io.modelcontextprotocol.sdk 2.0、Apache PDFBox 3.0.5（待确认）、Apache Tika BOM 3.2.2、io.github.easy4j:tts-edge-java 1.3.3、io.github.easy4j:whispercpp 1.4.0、JUnit 6.1、Mockito 5.23、AssertJ、Testcontainers（必要时）。

**Related Design Doc:**
- `2026-08-12-mcp-component-design.md`
- `2026-08-12-ocr-component-design.md`
- `2026-08-12-agent-component-design.md`
- `2026-08-12-flow-component-design.md`
- `2026-08-12-router-component-design.md`
- `2026-08-12-asr-component-design.md`
- `2026-08-12-tts-component-design.md`

## Global Constraints

- 每个组件按 TDD 顺序：ContractTest（fake/桩）→ impl → AutoConfigTest → 必要的容器 IT
- 不修改既有 5 组件（core/sst/chat/memory/embedding/vectordb/rag）的代码；只新增各自模块
- Spring AI 客户端 starter 仅 test-scope 引入（保持组件本体不绑供应商）；端口通过 `@ConditionalOnBean` 触发
- AutoConfiguration 加 `@AutoConfiguration(afterName=...)` 处理装配顺序（参考 chat/embedding/vectordb）
- 依赖引入统一走 ddd4j-ai-dependencies BOM（已在 properties 管理版本），模块 pom 直接引用
- 容器类 IT 全部 `disabledWithoutDocker = true` 优雅跳过
- 提交约定：conventional commits；feature 分支开发，feature/2.0.x 同步推进 feature/1.0.x cherry-pick
- tests 必须在 `./mvnw test -DskipTests=false` 全绿（`-Denforcer.skip=true` 是因 enforcer 3.6.3 与 Maven 4-rc6 不兼容的工具链问题）

## Task 1: mcp（Model Context Protocol）

- [ ] **Step 1:** 创建 `ddd4j-ai-extension-mcp` 模块骨架（pom.xml + META-INF/spring/...AutoConfiguration.imports）
- [ ] **Step 2:** 端口 `McpToolProvider`（`List<ToolDefinition> availableTools()`）与 `McpToolExecutor`（`Object execute(tool, args)`）
- [ ] **Step 3:** 适配 `SpringAiMcpToolProvider`（委托 Spring AI MCP `McpSyncClient` / `McpAsyncClient`），`@ConditionalOnBean({McpSyncClient.class, McpAsyncClient.class})`
- [ ] **Step 4:** `McpAutoConfiguration` + 装配顺序（after Spring AI MCP auto-config）
- [ ] **Step 5:** ContractTest（fake tool 定义与执行）+ AutoConfigTest
- [ ] **Step 6:** 端到端 IT（spring-ai-starter-mcp-server-webmvc + Testcontainers Ollama 客户端，CI 跳过）

## Task 2: ocr（文档/图像识别）

- [ ] **Step 1:** 创建 `ddd4j-ai-extension-ocr` 模块；pom 引入 PDFBox 3.x + Apache Tika 3.2.2
- [ ] **Step 2:** 端口 `OcrService`（`String extractText(InputStream, MediaType)`、`List<Document> extract(InputStream, MediaType)`）
- [ ] **Step 3:** `PdfBoxOcrService`（PDF 文本提取）+ `TikaOcrService`（多格式解析：Office/HTML/图像元数据/OCR 调用 Tesseract 框架）
- [ ] **Step 4:** `OcrAutoConfiguration` + `@ConditionalOnClass({PDFBox.class, Tika.class})` 切换策略
- [ ] **Step 5:** ContractTest（fake 字节流 + 不同 MediaType 分发）+ AutoConfigTest
- [ ] **Step 6:** IT（Testcontainers 真实 PDF/Office 文件，无 Docker 跳过）

## Task 3: agent（智能体编排）

- [ ] **Step 1:** 创建 `ddd4j-ai-extension-agent` 模块；pom 引入 `spring-ai-commons` + `spring-ai-client-chat`
- [ ] **Step 2:** 端口 `AgentService`（`AgentResult execute(AgentTask)`、`Flux<AgentStep> stream(AgentTask)`）
- [ ] **Step 3:** `ReActAgent`（Thought→Action→Observation 循环，支持 ChatService + MemoryService + 可注入 List<ToolCallback>）
- [ ] **Step 4:** `PlanExecuteAgent`（先规划后执行；支持多步骤）
- [ ] **Step 5:** `AgentAutoConfiguration` + `@ConditionalOnBean(ChatService.class)`，可选 MemoryService/ToolCallback 注入
- [ ] **Step 6:** ContractTest（fake 推理循环）+ AutoConfigTest
- [ ] **Step 7:** IT（Testcontainers Ollama 真模型集成 chat+agent，按 classpath 探测跳过）

## Task 4: flow（AI 工作流编排）

- [ ] **Step 1:** 创建 `ddd4j-ai-extension-flow` 模块；pom 引入 `com.alibaba.cloud.ai:spring-ai-alibaba-graph-core:2.0.0-M1.1`
- [ ] **Step 2:** 端口 `FlowService`（`<I,O> StateGraph<I,O> buildGraph(FlowDef)`、`O run(CompiledGraph<I,O>, I)`）
- [ ] **Step 3:** 节点抽象：`LlmNode`（调 ChatService）、`BranchNode`（条件分支）、`LoopNode`（循环）、`ToolNode`（调 List<ToolCallback>）
- [ ] **Step 4:** `FlowAutoConfiguration` + `@ConditionalOnBean(ChatService.class)`
- [ ] **Step 5:** ContractTest（伪 Flow 执行）+ AutoConfigTest
- [ ] **Step 6:** IT（Testcontainers Ollama 真模型驱动 LlmNode 完整链路）

## Task 5: router（多模型路由与负载）

- [ ] **Step 1:** 创建 `ddd4j-ai-extension-router` 模块；pom 仅依赖 spring-ai-commons（不绑厂商）
- [ ] **Step 2:** 端口 `ChatRouter`（`String route(AiRequest)`、`Flux<String> streamRoute(AiRequest)`）
- [ ] **Step 3:** 策略接口 `RoutingStrategy`（`String select(List<String> modelCandidates)`）与实现：
  - `RoundRobinStrategy`、`WeightedStrategy`、`LatencyStrategy`（响应时间估算，可选）
- [ ] **Step 4:** `MultiModelChatRouter`（委托 ChatClient.Builder 池按策略选）
- [ ] **Step 5:** `RouterAutoConfiguration` + `@ConditionalOnBean(ChatClient.Builder.class)`
- [ ] **Step 6:** ContractTest（策略选择正确性）+ AutoConfigTest（多模型候选时按策略分发）

## Task 6: asr（离线语音识别 WhisperCpp）

- [ ] **Step 1:** 创建 `ddd4j-ai-extension-asr` 模块；pom 引入 `io.github.easy4j:whispercpp:1.4.0`
- [ ] **Step 2:** 端口 `AsrService`（`String transcribe(byte[] audio, AudioFormat fmt)`、`String transcribe(File audio, AudioFormat fmt)`）
- [ ] **Step 3:** `WhisperCppAsrService`（JNI 调用，含模型加载懒加载 + 16kHz 单声道 WAV 重采样）
- [ ] **Step 4:** `AsrAutoConfiguration` + `@ConditionalOnClass(io.github.ggerganov.whispercpp.WhisperContext.class)`（测试用 assumeTrue 探测 native）
- [ ] **Step 5:** ContractTest（fake 字节流）+ AutoConfigTest
- [ ] **Step 6:** IT（Testcontainers 跑 whisper.cpp 容器，含模型预拉，无 Docker 跳过）

## Task 7: tts（免费在线 TTS Edge TTS）

- [ ] **Step 1:** 创建 `ddd4j-ai-extension-tts` 模块；pom 引入 `io.github.easy4j:tts-edge-java:1.3.3`
- [ ] **Step 2:** 端口 `TtsService`（`byte[] synthesize(String text, String voice)`、`Flux<byte[]> streamSynthesize(String text, String voice)`）
- [ ] **Step 3:** `EdgeTtsService`（调 Microsoft Edge TTS 免费公共服务，处理 mp3/aac 流）
- [ ] **Step 4:** `TtsAutoConfiguration` + `@ConditionalOnClass(io.github.whitemagic2014.tts.EdgeTTS.class)`
- [ ] **Step 5:** ContractTest（fake HTTP 响应）+ AutoConfigTest
- [ ] **Step 6:** IT（Testcontainers 模拟 HTTP 服务，无 Docker 跳过；真实场景需外网）

## Task 8: spec/plan 文档回写与 README 更新

- [ ] **Step 1:** 6 个 spec 文件（mcp/ocr/agent/flow/router/asr/tts）状态：待实施 → 已实施（含 commit SHA）
- [ ] **Step 2:** v1.x-B 与 v2.0 plans 状态更新（已勾选步骤 + 实施日期）
- [ ] **Step 3:** 架构 spec 组件模块状态总表更新（mcp/ocr/agent/flow/router/asr/tts 全为已实现）
- [ ] **Step 4:** README 路线图与快速开始补齐 6 个 sample（沿用 SstSample 风格）

## Task 9: 全量构建与发布

- [ ] **Step 1:** `./mvnw -U -Denforcer.skip=true clean test` 全 reactor 测试全绿
- [ ] **Step 2:** `./mvnw -U -Denforcer.skip=true deploy` 发布到私仓
- [ ] **Step 3:** 双分支推送（cherry-pick master 到 feature/2.0.x；feature/1.0.x 走 Boot3.4 线仅 cherry-pick 与版本管理相关的修改）
- [ ] **Step 4:** GitHub Actions CI 触发（org 计费问题请用户修复后自动验证）

## Self-Review 结论

- **Spec coverage:** 6 个 spec 各对应一个 Task，每个 spec 的端口契约由 Step 5 的 ContractTest 验证。✅
- **Placeholder scan:** 无 TODO/TBD（contract test 在 impl 前以失败断言就位）。✅
- **Type consistency:** 端口命名后缀统一（Service/Provider/Router/Executor），impl 后缀（Adapter/Service）。✅
