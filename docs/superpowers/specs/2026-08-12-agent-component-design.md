# Agent 智能体编排组件设计

- 日期：2026-08-12
- 作者：PartMe.AI
- 状态：待实施（骨架：仅 package-info 占位）
- 范围：`ddd4j-ai-extension-agent` —— 智能体编排与工具调用
- 关联文档：整体架构见 `2026-08-07-ddd4j-ai-architecture-design.md`；依赖 chat/memory/mcp

---

## 1. 背景

Agent 是 LLM 从"对话"走向"行动"的关键能力。agent 组件提供智能体编排框架，支持 ReAct、Plan-and-Execute 等模式，通过 MCP 协议调用外部工具，并与 chat（推理）+ memory（上下文）协同。

## 2. 目标

- 提供智能体编排端口（任务 → 思考 → 工具调用 → 结果）。
- 支持 ReAct 与 Plan-and-Execute 模式。
- 通过 mcp 组件接入标准化工具。
- 对接 AgentScope Java BOM（2.0.1）作为可选编排运行时。

### 非目标

- 不内置具体工具实现（工具由业务或 mcp 暴露）。
- 不替代 Spring AI 的 function calling（在其之上做编排封装）。

## 3. 关键决策（建议）

| # | 决策候选 | 理由 |
|---|---------|------|
| A1 | 编排模式可插拔（ReAct/Plan-Execute 策略接口） | 不同任务复杂度选不同模式 |
| A2 | 工具调用统一走 mcp 协议端口 | 标准化、可跨进程 |
| A3 | 上下文走 memory 组件 | 复用会话记忆 |

## 4. 总体架构

```
业务 → AgentService（端口）→ 编排策略（ReAct/PlanExecute）→ {ChatService 推理, McpService 工具, MemoryService 记忆}
```

## 5. 接口方向

- `AgentService`：`run(AgentTask)` → `AgentResult`；支持中间步骤回调/观察。

## 6. 模块落点

```
ddd4j-ai-extension-agent/src/main/java/io/ddd4j/ai/cmpt/agent
├── dto/vo/ (AgentTask, AgentResult, AgentStep)
├── properties/
├── service/AgentService.java
└── service/impl/{ReAct,PlanExecute}Agent.java
```

## 7. 依赖

- ddd4j-ai-extension-chat / memory / mcp
- Spring AI 2.0.0（function calling）
- 可选：AgentScope Java 2.0.1（编排运行时）

## 8. 测试策略

- 编排策略单测（mock 推理与工具）；工具调用回路测试；多轮 ReAct 循环终止条件测试。
