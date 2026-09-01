# ddd4j-ai-extension-agent v2：替换为 Agentscope 域模型（保留 AgentService SPI 作为薄包装）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 2.0.x 上将 `ddd4j-ai-extension-agent` 替换为 **Agentscope-Java 2.0.0** 实现（参考 cloud-agents 的 `AgentScopeHarnessFactory` 实际架构），保留 `AgentService` SPI 作为业务调用方稳定入口；删除自实现 ReAct/Plan-Execute 内部类。验证后 cherry-pick 到 1.0.x。

**关键决策（基于 2026-09-02 探索事实）**：
- cloud-agents 实际**已在用 AgentScope**（README 第一句"OpenClaw"误导），其 `AgentScopeHarnessFactory` 模式：业务编排层（Java/DDD）→ AgentScope Harness（Java 库）→ OpenClaw 节点运行实例。本计划按此范式：保留业务 SPI（`AgentService`），删除自实现引擎（`ReActAgent`/`PlanExecuteAgent`），引入 AgentScope Beans 作为默认实现。
- `AgentService` SPI **保留**（flow/router/samples 仍依赖）；Agentscope 原生类（`ReActAgent`、`Toolkit`、`Msg`、`MsgHub`、`HarnessAgent`）作为 **Spring Beans** 暴露，业务按需取用。
- 自实现 `ReActAgent`/`PlanExecuteAgent` 内部类**删除**（被 Agentscope `ReActAgent` 替代）；对应的 9 个内部测试**删**（改写为对 Agentscope Beans 的契约测试 + AgentService 适配测试）。
- `AgentService`/`AgentTask`/`AgentStep`/`AgentResult` 契约**不变**，由 `AgentScopeAgentAdapter implements AgentService` 包装 Agentscope `ReActAgent` 调用结果，保持调用方零感知。

**关键 API（已核实于 agentscope-core 2.0.0 jar，2026-09-02）**：
- `io.agentscope.core.ReActAgent`（顶层 API + Builder）
- `io.agentscope.core.tool.Toolkit` + `io.agentscope.core.tool.Tool`
- `io.agentscope.core.message.Msg`（message 协议单元）
- `io.agentscope.core.memory.Memory` / `InMemoryMemory`
- `io.agentscope.core.MsgHub`（多智能体通信）
- `io.agentscope.harness.agent.HarnessAgent`（含 subagent 委托；harness 子包）
- `io.agentscope.extensions.model.openai.OpenAIChatModel`（OpenAI 兼容 Model provider）
- deps BOM 已 import `agentscope-bom:2.0.2`（`agentscope-java.version`）；本地仓库已缓存 2.0.0 jar

**Architecture（替换后）**：
```
[业务调用方: flow/router/samples] → AgentService SPI（不变）
                                       ↓
                                  AgentScopeAgentAdapter（薄包装，实现 AgentService）
                                       ↓
   ┌──────────────────┬──────────────────┬──────────────────┐
   │ ReActAgent       │ HarnessAgent      │ Memory/MsgHub     │ （Agentscope 原生）
   │ (core/Builder)   │ (harness/)        │ (core/)           │
   └──────────────────┴──────────────────┴──────────────────┘
                                       ↓
                            SpringAiChatModelAdapter
                                       ↓
                          ChatService (ddd4j-ai-extension-chat)
                                       ↓
                            Spring AI ChatClient → 任意 LLM
```

**Tech Stack:** Java 17、Maven 3（Maven 4 modelVersion）、Spring AI 2.0、agentscope-core/harness/extensions-model-openai 2.0.0、Spring Boot AutoConfigure。

## Global Constraints

- **公共契约零破坏**：`AgentService`/`AgentTask`/`AgentStep`/`AgentResult` 签名不动；`AgentAutoConfiguration` 装配 bean 名不变；`@Primary` 切换为 Agentscope 实现
- **删除清单最小化**：仅删 `ReActAgent.java`/`PlanExecuteAgent.java`（2 个内部类）+ `ReActAgentTest.java`/`PlanExecuteAgentTest.java`（2 个内部测试）；其余 13 个 Agent 模块文件保留（service/AutoConfiguration/properties）
- **基线快照可回退**：plan 基线快照已提交（`2bf37f3`）——任何 Task 失败可 `git revert HEAD..<commit>` 回退
- **同步模块影响最小化**：仅 `flow`/`router`/`samples` 需验证（字符串装配 `afterName=io.ddd4j.ai.cmpt.agent.autoconfigure.AgentAutoConfiguration` 经包名重构后为 `...extension.agent.autoconfigure.AgentAutoConfiguration`，不动）；`document/mcp/rag/chat/embedding/vectordb/memory/asr/tts/flow/sst/router/ocr` **不**改
- **依赖零新增**：Agentscope 已在 deps BOM 管理（`agentscope-bom:2.0.2`），子模块通过 BOM 自动解析版本
- **测试零依赖真实 LLM**：用 Mockito 模拟 Agentscope `Model`/`Toolkit`/`Memory`，不联网；集成测试可后续按 `@EnabledIfDockerAvailable` 启用
- **提交规范**：conventional commits；每 Task 独立提交；最终推送双远程
- **验证命令**：`./mvnw -U -Denforcer.skip=true -B -DskipTests=false -pl ddd4j-ai-extensions/ddd4j-ai-extension-agent -am test -Dsurefire.failIfNoSpecifiedTests=false` 必须 BUILD SUCCESS

---

### Task 1: 依赖升级 + agentscope-core compile scope

**Files:**
- Modify: `ddd4j-ai-extensions/ddd4j-ai-extension-agent/pom.xml`
- Test: 重跑 `AgentAutoConfigurationTest`（验证 spring bean 装配）

**Interfaces:**
- `agentscope-core` `provided → compile`（业务调用方可用，需传递）
- 新增 `agentscope-harness` `compile`
- 新增 `agentscope-extensions-model-openai` `compile`（OpenAI 兼容 Model 提供商，含 DashScope/DeepSeek/Ollama 等）

- [ ] **Step 1: 修改 pom.xml**：把 `provided` 改 `compile` 并加 2 个 compile 依赖
- [ ] **Step 2: 运行** `./mvnw -U -Denforcer.skip=true -B -DskipTests=false -pl ddd4j-ai-extensions/ddd4j-ai-extension-agent -am test`
- [ ] **Step 3: 确认 BUILD SUCCESS**（现有 17 测试保持全绿）
- [ ] **Step 4: Commit** `build(agent): promote agentscope to compile scope + add harness + openai extensions`

---

### Task 2: SpringAiChatModelAdapter — ChatService → Agentscope Model

**Files:**
- Create: `src/main/java/io/ddd4j/ai/extension/agent/agent/SpringAiChatModelAdapter.java`
- Test: `src/test/java/io/ddd4j/ai/extension/agent/agent/SpringAiChatModelAdapterTest.java`

**Interfaces:**
- `public final class SpringAiChatModelAdapter implements io.agentscope.core.model.Model`（参考 cloud-agents `RuntimeChatClient`，用 `ChatService` 作为底层）
- 暴露构造：`SpringAiChatModelAdapter(ChatService chatService, Memory memory)` — Memory 用于上下文历史注入
- `reply(Msg... msgs)` / `stream(Msg...)` — 委托 `chatService.chat()`，按 Agentscope `Msg` 协议组装（user→ChatService.userMessage，assistant→ChatService 响应）
- **降级**：Agentscope 调用 `reply()` 抛异常时 → 包装为 `AgentExecutionException`，不静默吞

- [ ] **Step 1: 写失败测试** `SpringAiChatModelAdapterTest`：
  - `reply_userMsg_returnsAssistantMsg`
  - `reply_emptyMessages_throws`
  - `reply_chatServiceFails_throwsAgentExecution`
  - `stream_emitsChunks`
- [ ] **Step 2: 运行确认失败**（类不存在）
- [ ] **Step 3: 实现 `SpringAiChatModelAdapter`**
- [ ] **Step 4: 验证 4 测试通过 + Commit** `feat(agent): add SpringAiChatModelAdapter for ChatService→Model integration`

---

### Task 3: AgentScopeAgentAdapter — AgentService SPI 薄包装

**Files:**
- Create: `src/main/java/io/ddd4j/ai/extension/agent/agent/AgentScopeAgentAdapter.java`
- Modify: `src/main/java/io/ddd4j/ai/extension/agent/service/impl/ReActAgent.java` → 删除
- Modify: `src/main/java/io/ddd4j/ai/extension/agent/service/impl/PlanExecuteAgent.java` → 删除
- Test: 新增 `src/test/java/io/ddd4j/ai/extension/agent/agent/AgentScopeAgentAdapterTest.java`

**Interfaces:**
- `public final class AgentScopeAgentAdapter implements AgentService`（**不**带具体策略后缀；执行策略由构造注入的 `ReActAgent`/`HarnessAgent` 实例决定）
- 构造：`AgentScopeAgentAdapter(ReActAgent reactAgent)` 用于 ReAct 模式 + `AgentScopeAgentAdapter(HarnessAgent harnessAgent)` 用于多智能体模式
- `execute(AgentTask)` → 委托 Agentscope `reply(Msg.userMessage(instruction))`，将响应转为 `AgentResult`（响应文本→output，agentscope 中间事件→`AgentStep.type="thought"|"observation"|"result"`）
- `stream(AgentTask)` → 委托 `ReActAgent.stream(Msg...)`，映射为 `Flux<AgentStep>`
- **不**保留 Plan-Execute 单独实现（HarnessAgent 内部含 planning 逻辑）

- [ ] **Step 1: 写失败测试** `AgentScopeAgentAdapterTest`（5 个）：
  - `execute_returnsAgentResultWithMarkdown`
  - `execute_extractsToolObservationsAsSteps`
  - `execute_maxIterationsExceeded_throwsAgentExecution`
  - `stream_emitsReActSteps`
  - `stream_handlesAdapterFailureAsError`
- [ ] **Step 2: 实现 `AgentScopeAgentAdapter`**
- [ ] **Step 3: 删除 `ReActAgent.java`/`PlanExecuteAgent.java` + 删除 `ReActAgentTest.java`/`PlanExecuteAgentTest.java`**
- [ ] **Step 4: 验证 + Commit** `feat(agent): replace self-implemented ReAct/Plan-Execute with Agentscope adapter (preserves AgentService SPI)`

---

### Task 4: AgentScopeToolAdapter — ToolCallback → Agentscope @Tool

**Files:**
- Create: `src/main/java/io/ddd4j/ai/extension/agent/agent/AgentScopeToolAdapter.java`
- Test: `src/test/java/io/ddd4j/ai/extension/agent/agent/AgentScopeToolAdapterTest.java`

**Interfaces:**
- `public final class AgentScopeToolAdapter` 工具类（参考 cloud-agents `AgentScopeOpenClawToolAdapter`）
- `public static Tool toTool(ToolCallback callback)` — 把 `org.springframework.ai.tool.ToolCallback`（Spring AI 接口）适配为 `io.agentscope.core.tool.Tool`（Agentscope 接口）
- `public static Toolkit.Builder register(Toolkit.Builder builder, List<ToolCallback> callbacks)` — 批量注册
- **降级**：调用 Spring AI `ToolCallback.call(input)` 抛异常时 → `ToolResultBlock.fail(errorMsg)`

- [ ] **Step 1: 写失败测试** `AgentScopeToolAdapterTest`（4 个）：
  - `toTool_invokesCallback`
  - `toTool_callbackError_returnsFailBlock`
  - `register_multipleCallbacks_buildsToolkit`
  - `toTool_nullCallback_throws`
- [ ] **Step 2: 实现**
- [ ] **Step 3: 验证 + Commit** `feat(agent): adapt Spring AI ToolCallback to Agentscope Tool/Toolkit`

---

### Task 5: AgentScopeAgentAutoConfiguration — 暴露 Agentscope Beans

**Files:**
- Modify: `src/main/java/io/ddd4j/ai/extension/agent/autoconfigure/AgentAutoConfiguration.java`
- Test: 修改 `AgentAutoConfigurationTest` 验证 Agentscope Beans 装配

**Interfaces:**
- `@Bean @ConditionalOnMissingBean public ReActAgent.Builder reactAgentBuilder(SpringAiChatModelAdapter model, ObjectProvider<List<ToolCallback>> toolCallbacks, AgentProperties properties)` — 工厂 Bean（参考 cloud-agents `AgentScopeHarnessFactory.java:41-78`）
- `@Bean @ConditionalOnMissingBean public Memory defaultMemory()` — InMemoryMemory（可被业务覆盖）
- `@Bean @ConditionalOnMissingBean public AgentService agentService(ReActAgent.Builder builder)` — 用 Builder 构造默认智能体 + `AgentScopeAgentAdapter` 包装
- 装配顺序：`@AutoConfiguration(afterName = ChatAutoConfiguration)` 不变
- **不** 删除既有 `AgentProperties`（maxIterations 等配置复用）

- [ ] **Step 1: 写失败测试** `AgentAutoConfigurationTest`（5 个）：
  - `defaultContext_exposesReactAgentBuilder`
  - `defaultContext_exposesMemory`
  - `defaultContext_exposesAgentService`
  - `highQualityDisabled_replacesBean`
  - `userProvidedAgentService_backsOff`
- [ ] **Step 2: 重写 `AgentAutoConfiguration.java`**（Agentscope Beans 装配）
- [ ] **Step 3: 验证 + Commit** `feat(agent): AutoConfiguration exposes Agentscope Beans (ReActAgent.Builder / Memory / AgentService)`

---

### Task 6: samples 模块 Agentscope 写法重写

**Files:**
- Modify: `ddd4j-ai-samples/src/main/java/io/ddd4j/ai/samples/agent/AgentSample.java`

**Interfaces:**
- 演示用 `ReActAgent.Builder` 直接构造智能体（不再依赖 `AgentService`）
- 演示 `@Tool` 注解注册工具（参考 cloud-agents `default-skills/baoyu-danger-gemini-web/SKILL.md` 风格）
- 不引入新测试（samples 不强制带测试）

- [ ] **Step 1: 重写 `AgentSample.java`**
- [ ] **Step 2: 全量编译验证**（`./mvnw -pl ddd4j-ai-samples compile`）
- [ ] **Step 3: Commit** `refactor(samples): rewrite AgentSample with Agentscope Builder style`

---

### Task 7: 全量验证 + 文档回写 + 双分支推送

- [ ] **Step 1: 全量 reactor 验证**
  - `./mvnw -U -Denforcer.skip=true -B -DskipTests=false clean test` 23 模块 BUILD SUCCESS
- [ ] **Step 2: 集成测试**（可选真实 Ollama @EnabledIfDockerAvailable）— 超计划范围可后续
- [ ] **Step 3: docs 回写**
  - 在 `docs/superpowers/plans/` 新建 `2026-09-02-agent-agentscope-replacement.md` 的实施记录
  - `docs/superpowers/specs/2026-08-12-agent-component-design.md` 状态：`待实施` → `已实现（Agentscope）`
- [ ] **Step 4: 推送双远程**（feature/2.0.x）
- [ ] **Step 5: cherry-pick 到 feature/1.0.x**（包名 `extension`，按 1.0.x 提交 `5cbc9ea` 后的状态；冲突需手动解决 pom 差异）

---

## Self-Review

- **能力覆盖**：replacement 后 5 大能力（ReAct/tool-calling/memory/RAG/multi-agent）全部由 Agentscope 提供：
  - ReAct：Agentscope `ReActAgent` ✓
  - Tool calling：`AgentScopeToolAdapter`（Spring AI ToolCallback 兼容）✓
  - Memory：Agentscope `InMemoryMemory`（Bean）✓
  - RAG：保留 ddd4j-ai-extension-rag（Agentscope 也有 RAG toolkit，未来按需扩展）
  - Multi-Agent：Agentscope `HarnessAgent`（Bean）+ `MsgHub`（规划中）
- **公共契约零破坏**：`AgentService`/`AgentTask`/`AgentStep`/`AgentResult` 不动；`AgentAutoConfiguration` 装配 bean 名不变；调用方代码零改动
- **回退成本**：`git revert <commit>` 单 commit 即可恢复自实现路径（基线快照 `2bf37f3` 已 anchor）
- **测试影响**：17 → 13 测试类（删 2 个内部测试，新增 4 个适配器测试 + 1 个集成测试；总数减少但契约覆盖更聚焦）
- **风险面**：仅限 agent 模块 + samples 写入；flow/router/chat/memory/document/rag/mcp/asr/tts/sst/embedding/vectordb/flow/ocr/router **完全不动**
- **占位扫描**：无 TODO；Task 6 的集成测试明确标注「可选超计划」
