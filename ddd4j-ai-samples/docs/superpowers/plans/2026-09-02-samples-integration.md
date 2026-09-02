# ddd4j-ai-samples 集成示例完善计划

## 背景与目标

当前 ddd4j-ai-samples 有 13 个 1:1 组件示例（每个 Sample 对应一个 extension 模块），但缺少**集成示例**——同时串联多个组件的真实场景。参考 spring-ai-examples（49 模块，但**没有任何一个同时集成了 RAG + Tools + Memory**），我们有机会填补这个空白。

**目标**：在现有 13 个组件示例基础上，新增 7 个集成示例，覆盖：
1. 纯对话 + 记忆（基础但完整）
2. RAG 全链路（文档→解析→嵌入→向量库→检索→生成）
3. 智能体（ReAct 循环 + 工具调用 + MCP + 记忆）
4. 多模型路由（策略分发）
5. 文档理解（OCR + 文档解析 + 智能问答）
6. 工作流编排（多节点 graph）
7. 多智能体协作（orchestrator + 子智能体派发）

## 架构设计

### 集成示例 vs 组件示例的关系

```
ddd4j-ai-samples/
  src/main/java/io/ddd4j/ai/samples/
    chat/ChatSample.java              ← 组件示例（已有）
    memory/MemorySample.java          ← 组件示例（已有）
    rag/RagSample.java                ← 组件示例（已有）
    ...
    integrated/                       ← 新增：集成示例目录
      chat-memory/                    ← 纯对话 + 记忆
      rag-pipeline/                   ← RAG 全链路
      agent-with-tools/               ← 智能体 + MCP + 工具
      multi-model-router/             ← 多模型路由
      document-understanding/         ← 文档理解
      workflow-orchestration/         ← 工作流编排
      multi-agent-dispatch/           ← 多智能体协作
```

每个集成示例是独立的 `@Component`，通过构造器注入多个端口接口，方法体展示完整业务流程。

### 约束

- 集成示例不引入新的第三方依赖（只用已有的 extension 模块）
- 每个示例有 Javadoc 说明组件串联关系
- application.yml 示例配置放在 `src/main/resources/application-integrated.yml`（参考文件）
- 全部示例编译通过（`mvn compile`），不强制有集成测试（需要真实 LLM）

---

## Task 1: 创建 integrated/ 目录结构 + chat-memory 示例

**目标**：最基础的集成——多轮对话 + 记忆（展示 chat + memory 串联）

**Files:**
- Create: `integrated/chat-memory/ChatMemorySample.java`
- Modify: `ddd4j-ai-samples/pom.xml`（确保 memory + chat 依赖已有）

**核心逻辑：**
```java
@Component
public class ChatMemorySample {
    // 注入 ChatService + MemoryService
    // 方法1: 多轮对话（自动携带记忆）
    // 方法2: 流式多轮对话
    // 方法3: 手动管理记忆（clear/reset）
}
```

- [ ] Step 1: 创建目录 + ChatMemorySample.java
- [ ] Step 2: `mvn compile` 验证
- [ ] Step 3: Commit `feat(samples): add integrated chat-memory sample`

---

## Task 2: rag-pipeline 示例（文档→RAG 全链路）

**目标**：串联 document + embedding + vectordb + rag + chat，展示完整的 RAG 知识库流水线

**Files:**
- Create: `integrated/rag-pipeline/RagPipelineSample.java`
- Modify: `ddd4j-ai-samples/pom.xml`（确保 document 依赖已有）

**核心逻辑：**
```java
@Component
public class RagPipelineSample {
    // 注入 DocumentReader + EmbeddingService + VectorDbService + RagService + ChatService
    // 方法1: ingestDocument(File) → 解析文档 → 向量化 → 存入向量库
    // 方法2: ingestFromOcr(byte[]) → OCR 提取文本 → 向量化 → 存入
    // 方法3: askWithRag(question) → 检索 + 生成
    // 方法4: askWithRagStream(question) → 流式 RAG
}
```

- [ ] Step 1: 创建 RagPipelineSample.java
- [ ] Step 2: `mvn compile` 验证
- [ ] Step 3: Commit `feat(samples): add integrated rag-pipeline sample`

---

## Task 3: agent-with-tools 示例（智能体 + MCP + 工具 + 记忆）

**目标**：最复杂的集成——智能体编排 + MCP 工具 + 本地工具 + 记忆（参考 spring-ai-examples 缺失的 RAG+Tools+Memory 集成）

**Files:**
- Create: `integrated/agent-with-tools/AgentWithToolsSample.java`

**核心逻辑：**
```java
@Component
public class AgentWithToolsSample {
    // 注入 HarnessAgent + McpToolProvider + AgentService + MemoryService
    // 方法1: runWithTools(instruction) → HarnessAgent 执行（含内置工具）
    // 方法2: runWithMcpTools(instruction) → 列出 MCP 工具 → 注入 Agent 上下文
    // 方法3: runWithMemory(instruction, conversationId) → 带记忆的智能体执行
    // 方法4: orchestratorDispatch(planId, tasks) → 多任务派发 + 合并
}
```

- [ ] Step 1: 创建 AgentWithToolsSample.java
- [ ] Step 2: `mvn compile` 验证
- [ ] Step 3: Commit `feat(samples): add integrated agent-with-tools sample (RAG+Tools+Memory)`

---

## Task 4: multi-model-router 示例

**目标**：多模型路由策略（轮询/权重/延迟）

**Files:**
- Create: `integrated/multi-model-router/MultiModelRouterSample.java`

**核心逻辑：**
```java
@Component
public class MultiModelRouterSample {
    // 注入 ChatRouter + ChatService
    // 方法1: route(request) → 按策略选择模型并执行
    // 方法2: routeStream(request) → 流式路由
    // 方法3: demonstrateStrategies() → 展示不同策略的行为差异
}
```

- [ ] Step 1: 创建 MultiModelRouterSample.java
- [ ] Step 2: `mvn compile` 验证
- [ ] Step 3: Commit `feat(samples): add integrated multi-model-router sample`

---

## Task 5: document-understanding 示例（OCR + 文档解析 + 智能问答）

**目标**：文档/图片 → OCR/解析 → 智能问答（串联 ocr + document + chat）

**Files:**
- Create: `integrated/document-understanding/DocumentUnderstandingSample.java`

**核心逻辑：**
```java
@Component
public class DocumentUnderstandingSample {
    // 注入 OcrService + DocumentReader + ChatService
    // 方法1: ocrAndAsk(imageBytes, question) → OCR 提取文字 → 注入 Chat 上下文 → 回答
    // 方法2: parseAndAsk(file, question) → 解析文档 → 注入 Chat → 回答
    // 方法3: extractStructuredData(file) → 解析文档 → 结构化提取
}
```

- [ ] Step 1: 创建 DocumentUnderstandingSample.java
- [ ] Step 2: `mvn compile` 验证
- [ ] Step 3: Commit `feat(samples): add integrated document-understanding sample`

---

## Task 6: workflow-orchestration 示例

**目标**：工作流编排——多节点 graph（LLM/Tool/Branch）

**Files:**
- Create: `integrated/workflow-orchestration/WorkflowOrchestrationSample.java`

**核心逻辑：**
```java
@Component
public class WorkflowOrchestrationSample {
    // 注入 FlowService
    // 方法1: buildSequentialFlow() → 构建顺序执行的 graph
    // 方法2: buildBranchingFlow() → 构建条件分支的 graph
    // 方法3: runWithAgentNode() → graph 中包含 Agent 节点
}
```

- [ ] Step 1: 创建 WorkflowOrchestrationSample.java
- [ ] Step 2: `mvn compile` 验证
- [ ] Step 3: Commit `feat(samples): add integrated workflow-orchestration sample`

---

## Task 7: multi-agent-dispatch 示例

**目标**：多智能体协作——orchestrator + 子智能体派发 + 结果合并

**Files:**
- Create: `integrated/multi-agent-dispatch/MultiAgentDispatchSample.java`

**核心逻辑：**
```java
@Component
public class MultiAgentDispatchSample {
    // 注入 AgentPlanOrchestrator + HarnessAgent + AgentDispatchTaskRepository
    // 方法1: submitAndDispatch(goal, taskInstructions) → 提交计划 → 派发 → 合并
    // 方法2: queryTaskStatus(planId) → 查询任务状态
    // 方法3: runWithSubagents(instruction) → 父智能体 + 声明式子智能体
}
```

- [ ] Step 1: 创建 MultiAgentDispatchSample.java
- [ ] Step 2: `mvn compile` 验证
- [ ] Step 3: Commit `feat(samples): add integrated multi-agent-dispatch sample`

---

## Task 8: application-integrated.yml 参考配置 + README 更新

**目标**：提供集成示例的参考配置文件 + 更新 README

**Files:**
- Create: `src/main/resources/application-integrated.yml`
- Modify: `ddd4j-ai-samples/README.md`（或项目根 README 的 samples 章节）

**application-integrated.yml 内容：**
```yaml
# ddd4j-ai 集成示例参考配置
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: qwen2.5:0.5b
      embedding:
        options:
          model: all-minilm

ddd4j:
  ai:
    chat:
      enabled: true
      default-system-prompt: "你是一个有用的AI助手"
    memory:
      enabled: true
      window-size: 20
    embedding:
      enabled: true
    vectordb:
      enabled: true
    rag:
      enabled: true
    agent:
      enabled: true
      api-key: ${OPENAI_API_KEY:}
      model-name: gpt-4o-mini
    document:
      enabled: true
    ocr:
      enabled: true
```

- [ ] Step 1: 创建 application-integrated.yml
- [ ] Step 2: 更新 README（列出集成示例清单 + 使用说明）
- [ ] Step 3: Commit `docs(samples): add integrated examples reference config and README`

---

## Task 9: 全量验证 + 双分支推送

- [ ] Step 1: `mvn -U -Denforcer.skip=true -B -DskipTests=false clean test` 全 reactor BUILD SUCCESS
- [ ] Step 2: `git push github feature/2.0.x && git push origin feature/2.0.x`
- [ ] Step 3: cherry-pick → feature/1.0.x（pom 冲突解决为 4.0.0 格式）
- [ ] Step 4: 1.0.x 全量验证 + 推送

---

## Self-Review

- **填补空白**：spring-ai-examples 没有 RAG+Tools+Memory 集成——我们的 agent-with-tools 示例直接填补
- **7 个集成示例**覆盖了 ddd4j-ai 的全部核心能力域（chat/memory/embedding/vectordb/rag/mcp/ocr/document/agent/flow/router/asr/tts/mcp）
- **零新增依赖**：全部基于已有 extension 模块
- **编译验证**：每个 Task 独立 commit + mvn compile 验证
- **向后兼容**：不修改现有 13 个组件示例
