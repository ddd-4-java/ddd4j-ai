# ddd4j-ai-extension-agent v2.1：替换为 Agentscope 域模型（修正版，按 cloud-agents 实际 API）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**修订背景（2026-09-02 关键发现）**：plan 初稿基于错误的 Agentscope API 假设（`Model.reply()`/`Msg.builder().textContent()`/`TextContent.of()`），实际 agentscope-core 2.0.0 jar 中这些 API 形态完全不同。本计划按 cloud-agents 真实代码路径修订：

| 旧 plan 假设 | 实际 API（已核实） |
|------------|----------------|
| `Model.reply(Msg...)` 同步接口 | **不存在** `reply`；`Model.stream(List<Msg>, List<ToolSchema>, GenerateOptions) → Flux<ChatResponse>` 才是 |
| `Msg.builder().role(USER).textContent("hi")` | 实际是 `UserMessage extends Msg` 构造 `(String)` |
| `TextContent.of(chunk)` | **不存在** `TextContent` 子类；`ContentBlock` 是普通类，文本走 `AssistantMessage(String)` |
| `Toolkit.Builder` | `Toolkit.registerTool(AgentTool)` |
| `Tool` 是接口 | 实际 `io.agentscope.core.tool.AgentTool`（类）+ `ToolCallParam` + `ToolResultBlock` |
| 适配 Model 接口再被 `ReActAgent.Builder` 消费 | 实际 cloud-agents **不写 Model 适配**，直接构造 `HarnessAgent`（含 plan/replay/subagent）+ 用 `OpenAIChatModel` |

**Goal:** 在 2.0.x 上将 `ddd4j-ai-extension-agent` 默认实现替换为 **Agentscope-Java 2.0.0**（参考 cloud-agents `AgentScopeHarnessFactory` 实际架构），保留 `AgentService` SPI 作为业务调用方稳定入口；删除自实现 `ReActAgent`/`PlanExecuteAgent`。验证后 cherry-pick 到 1.0.x。

**关键决策**：
- 严格对齐 cloud-agents 实际 API 调用形态——避免 plan 初稿的"猜 API"
- 保留 `AgentService`/`AgentTask`/`AgentStep`/`AgentResult` 契约（flow/router/samples 仍依赖）；由 `AgentScopeAgentAdapter implements AgentService` 包装 Agentscope `HarnessAgent` 调用结果
- 自实现 `ReActAgent`/`PlanExecuteAgent` 内部类删除；对应 9 个内部测试改写为对 Agentscope Beans 的契约测试
- `SpringAiChatModelAdapter`（Model 适配器）**不**需要——Agentscope `HarnessAgent.Builder.model(String)` 支持字符串模型名（按 cloud-agents `AgentScopeHarnessFactory.java:67` 的 `.model(agent.getModelName())` 直接传字符串模型名），Model provider 由业务侧按 agentscope-extensions-model-openai 配置

**Architecture（替换后，与 cloud-agents 对齐）**：
```
[业务调用方: flow/router/samples] → AgentService SPI（不变）
                                       ↓
                                  AgentScopeAgentAdapter（薄包装，实现 AgentService）
                                       ↓
                          HarnessAgent.Builder.build()（含 ReAct + 多智能体 + 记忆压缩）
                                       ↓
                          .model(modelName) + .toolkit(toolkit) + .memory(memory) + .subagents(...)
                                       ↓
                          Model（OpenAIChatModel 或 SpringAiChatModelAdapter）
```

**Tech Stack:** Java 17、Maven 3、Spring AI 2.0、agentscope-core/harness/extensions-model-openai 2.0.0、Spring Boot AutoConfigure。

## 已核实 API（2026-09-02）

- `io.agentscope.core.message.Msg`：抽象基类，子类 `UserMessage(String)`/`AssistantMessage(String)`/`SystemMessage`/`ToolResultMessage`
- `io.agentscope.harness.agent.HarnessAgent extends Agent extends CallableAgent, StreamableAgent`
- `HarnessAgent.call(Msg) → Mono<Msg>`（同步入口）
- `HarnessAgent.stream(Msg, StreamOptions, Class<T>) → Flux<Event>`（事件流）
- `HarnessAgent.Builder.model(String)` 与 `.model(Model)`：传字符串或 Model provider
- `io.agentscope.core.tool.AgentTool`（类）：`getName/getDescription/getParameters/callAsync(ToolCallParam) → Mono<ToolResultBlock>`
- `ToolResultBlock.text(String)` / `.error(String)`：构造结果
- `io.agentscope.core.tool.Toolkit.registerTool(AgentTool)`：注册工具
- deps BOM 已 import `agentscope-bom:2.0.2`；本地缓存 2.0.0

## Global Constraints

- **公共契约零破坏**：`AgentService`/`AgentTask`/`AgentStep`/`AgentResult` 签名不动；`AgentAutoConfiguration` 装配 bean 名不变
- **删除清单最小化**：仅删 `ReActAgent.java`/`PlanExecuteAgent.java`（2 内部类）+ `ReActAgentTest.java`/`PlanExecuteAgentTest.java`（2 内部测试）
- **基线快照可回退**：`2bf37f3` 提供 revert 锚点
- **同步模块影响最小化**：仅 `flow`/`router`/`samples` 需验证（装配 bean 名不变）
- **零外部 LLM 依赖**：测试用 Mockito 模拟 HarnessAgent
- **提交规范**：conventional commits；每 Task 独立提交；最终推送双远程
- **验证命令**：`./mvnw -U -Denforcer.skip=true -B -DskipTests=false -pl ddd4j-ai-extensions/ddd4j-ai-extension-agent -am test -Dsurefire.failIfNoSpecifiedTests=false` 必须 BUILD SUCCESS

---

### Task 1: 依赖升级 + agentscope-core compile scope（**已完成，提交 fc45fc4**）

agentscope-core/harness/extensions-model-openai 已 compile；17 既有测试保持全绿。

- [x] ~~Step 1-4：pom 改 scope~~（已落地）

---

### Task 2: AgentScopeAgentAdapter — AgentService SPI 薄包装（基于真实 API）

**Files:**
- Create: `src/main/java/io/ddd4j/ai/extension/agent/agent/AgentScopeAgentAdapter.java`
- Create: `src/test/java/io/ddd4j/ai/extension/agent/agent/AgentScopeAgentAdapterTest.java`

**Interfaces（按 cloud-agents 实际 API 形态）**：
- `public final class AgentScopeAgentAdapter implements AgentService`
- 构造：`AgentScopeAgentAdapter(HarnessAgent harnessAgent)` — 业务侧构造 HarnessAgent 并注入 Model/Toolkit/Memory/Subagents 后传入
- `execute(AgentTask task)`：
  ```java
  UserMessage msg = new UserMessage(task.instruction());
  Msg response = harnessAgent.call(msg).block();  // CallableAgent.call → Mono<Msg>
  AssistantMessage assistant = (AssistantMessage) response;  // 实际返回类型
  return mapToResult(assistant, task);  // → AgentResult + steps
  ```
- `stream(AgentTask task)`：
  ```java
  Flux<Event> events = harnessAgent.stream(new UserMessage(task.instruction()), new StreamOptions());
  return events.map(event -> mapToStep(event));  // → Flux<AgentStep>
  ```
- 错误：`Mono`/`Flux` 抛异常 → 包 `AgentExecutionException extends RuntimeException`
- **不**实现 `Model` 接口（cloud-agents 也不实现）——直接用 `HarnessAgent.Builder` 在 AutoConfiguration 中装配

- [ ] **Step 1: 写失败测试** `AgentScopeAgentAdapterTest`（5 个）：
  - `execute_returnsAgentResultFromAssistantMessage`
  - `execute_toolCallEmittedAsObservationStep`
  - `execute_harnessCallFails_throwsAgentExecution`
  - `stream_emitsEventsAsSteps`
  - `constructor_nullHarness_throws`
- [ ] **Step 2: 运行确认失败**
- [ ] **Step 3: 实现 `AgentScopeAgentAdapter`**
- [ ] **Step 4: 验证 5 测试通过 + Commit** `feat(agent): add AgentScopeAgentAdapter wrapping HarnessAgent for AgentService SPI`

---

### Task 3: SpringAiToolkitBuilder — ToolCallback → Agentscope AgentTool（基于真实 API）

**Files:**
- Create: `src/main/java/io/ddd4j/ai/extension/agent/agent/SpringAiToolkitBuilder.java`
- Create: `src/test/java/io/ddd4j/ai/extension/agent/agent/SpringAiToolkitBuilderTest.java`

**Interfaces（参考 cloud-agents `AgentScopeOpenClawToolAdapter`）**：
- `public final class SpringAiToolkitBuilder` 工具类（不实例化）
- `public static Toolkit.Builder registerSpringAiTools(Toolkit.Builder builder, List<ToolCallback> callbacks)` — 批量注册 Spring AI 工具回调
- 内部适配类 `SpringAiAgentTool implements AgentTool`：
  - `getName()` → `ToolCallback.getToolDefinition().name()`
  - `getDescription()` → `ToolCallback.getToolDefinition().description()`
  - `getParameters()` → 解析 ToolCallback JSON schema（空 Map 兜底，tools 调用通过 input raw JSON）
  - `callAsync(ToolCallParam param)` → `Mono<ToolResultBlock>` 包装 `callback.call(input)`，失败 → `ToolResultBlock.error(msg)`
- 降级：单个工具注册失败 → 记录 WARN 跳过（不中断整个 Toolkit 装配）

- [ ] **Step 1: 写失败测试** `SpringAiToolkitBuilderTest`（4 个）：
  - `register_invokesCallback`
  - `register_callbackError_returnsErrorBlock`
  - `register_emptyList_returnsEmptyBuilder`
  - `register_brokenTool_doesNotThrow`
- [ ] **Step 2: 运行确认失败**
- [ ] **Step 3: 实现**
- [ ] **Step 4: 验证 + Commit** `feat(agent): register Spring AI ToolCallback as Agentscope AgentTool`

---

### Task 4: AgentScopeAgentAutoConfiguration — 暴露 HarnessAgent Bean

**Files:**
- Modify: `src/main/java/io/ddd4j/ai/extension/agent/autoconfigure/AgentAutoConfiguration.java`
- Modify: `src/test/java/io/ddd4j/ai/extension/agent/autoconfigure/AgentAutoConfigurationTest.java`

**Interfaces（参考 cloud-agents `AgentScopeHarnessFactory.java:41-78`）**：
- `@Bean @ConditionalOnMissingBean public HarnessAgent harnessAgent(AgentProperties properties, ObjectProvider<List<ToolCallback>> toolCallbacks, ObjectProvider<Model> modelProvider)`：
  ```java
  HarnessAgent.Builder builder = HarnessAgent.builder()
      .name(properties.getName())
      .maxIters(properties.getMaxIterations())
      .memory(new InMemoryMemory());
  // Model：优先业务注入，否则按 properties.modelName 字符串（OpenAI 兼容）
  modelProvider.ifAvailable(builder::model);
  if (modelProvider.getIfAvailable() == null) {
      builder.model(properties.getModelName());
  }
  // Toolkit：业务 ToolCallback → Agentscope AgentTool
  Toolkit toolkit = new Toolkit();
  SpringAiToolkitBuilder.registerSpringAiTools(toolkit, toolCallbacks.getIfAvailable(List.of()))
      .forEach(tool -> {});
  builder.toolkit(toolkit);
  return builder.build();
  ```
- `@Bean @ConditionalOnMissingBean public AgentService agentService(HarnessAgent harnessAgent)` — `new AgentScopeAgentAdapter(harnessAgent)`
- 装配顺序：`@AutoConfiguration(afterName = ChatAutoConfiguration)` 不变
- 保留 `AgentProperties` + 新增字段：`name`（默认 `"default-agent"`）、`modelName`（默认 `"gpt-4o-mini"`）

- [ ] **Step 1: 写失败测试** `AgentAutoConfigurationTest`（4 个）：
  - `defaultContext_exposesHarnessAgentBean`
  - `defaultContext_exposesAgentService`
  - `toolCallbacksInjected_registeredAsAgentscopeTools`
  - `userProvidedHarnessAgent_backsOff`
- [ ] **Step 2: 实现 `AgentAutoConfiguration`**
- [ ] **Step 3: 验证 + Commit** `feat(agent): AutoConfiguration exposes HarnessAgent + Toolkit (Spring AI ToolCallback compat)`

---

### Task 5: 删除自实现 + 测试调整

**Files:**
- Delete: `src/main/java/io/ddd4j/ai/extension/agent/service/impl/ReActAgent.java`
- Delete: `src/main/java/io/ddd4j/ai/extension/agent/service/impl/PlanExecuteAgent.java`
- Delete: `src/test/java/io/ddd4j/ai/extension/agent/service/impl/ReActAgentTest.java`
- Delete: `src/test/java/io/ddd4j/ai/extension/agent/service/impl/PlanExecuteAgentTest.java`
- Modify: `src/main/java/io/ddd4j/ai/extension/agent/service/AgentService.java`（加 @Deprecated 注释说明迁移到 Agentscope 实现）

- [ ] **Step 1: 删除 4 文件**
- [ ] **Step 2: 运行 `AgentServiceContractTest` + `AgentAutoConfigurationTest` 验证（应仍 13 个测试，删除内部测试后净减 9 → 13 测试全绿）**
- [ ] **Step 3: Commit** `refactor(agent): remove self-implemented ReAct/Plan-Execute (replaced by Agentscope HarnessAgent)`

---

### Task 6: samples 模块 Agentscope 写法重写

**Files:**
- Modify: `ddd4j-ai-samples/src/main/java/io/ddd4j/ai/samples/agent/AgentSample.java`

**Interfaces（参考 cloud-agents AgentScopeHarnessFactory）**：
- 演示用 `HarnessAgent.Builder` 直接构造 + `OpenAIChatModel.builder()...build()` 装配 Model + `Toolkit.registerTool()` 演示工具
- 不再依赖 `AgentService`（改用 Agentscope 原生调用）

- [ ] **Step 1: 重写 `AgentSample.java`**
- [ ] **Step 2: 全量编译**（`./mvnw -pl ddd4j-ai-samples compile`）
- [ ] **Step 3: Commit** `refactor(samples): rewrite AgentSample with HarnessAgent.Builder + OpenAIChatModel`

---

### Task 7: 全量验证 + docs 回写 + 双分支推送

- [ ] **Step 1: 全量 reactor 验证**
  - `./mvnw -U -Denforcer.skip=true -B -DskipTests=false clean test` 23 模块 BUILD SUCCESS
- [ ] **Step 2: docs 回写**
  - `docs/superpowers/specs/2026-08-12-agent-component-design.md` 状态：`待实施` → `已实现（Agentscope 2.0.0）`
  - 本 plan 头部加「实施记录」+ 「修订记录（v2.1）」说明 API 校正过程
- [ ] **Step 3: 推送双远程**（feature/2.0.x → github + codeup origin）
- [ ] **Step 4: cherry-pick 到 feature/1.0.x**（包名 `extension`；pom 模型 4.0.0；冲突手动解决）

---

## Self-Review

- **API 真实性**：所有引用的 Agentscope API 均通过 `javap` 在 agentscope-core-2.0.0.jar 验证（`UserMessage(String)`/`AssistantMessage(String)`/`HarnessAgent.Builder.model(String)`/`Toolkit.registerTool(AgentTool)`/`AgentTool.callAsync(ToolCallParam)`）
- **能力覆盖**：5 大能力全部由 Agentscope 提供（ReAct→HarnessAgent 内置；tool-calling→Toolkit；memory→InMemoryMemory；RAG→保留 ddd4j-ai-extension-rag，未来可挂 Agentscope RAG toolkit；multi-agent→HarnessAgent.subagents()）
- **公共契约零破坏**：`AgentService`/`AgentTask`/`AgentStep`/`AgentResult` 不动
- **回退成本**：单 commit `git revert` 即可
- **风险面**：仅 agent 模块 + samples 写入；其他 12 组件不动
- **vs 初稿差异**：删除了错误的 `SpringAiChatModelAdapter implements Model`（实际不需要）；改为 `SpringAiToolkitBuilder` 工具类（参考 cloud-agents `AgentScopeOpenClawToolAdapter`）；删除 `PlanExecuteAgent` 与 `PlanExecuteAgentTest`（Agentscope HarnessAgent 内置 plan 逻辑）
- **占位扫描**：无 TODO
