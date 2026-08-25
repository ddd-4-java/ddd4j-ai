# Flow AI 工作流组件设计

- 日期：2026-08-12
- 作者：PartMe.AI
- 状态：已实施（2026-08-25 组件补全批次）
- 范围：`ddd4j-ai-extension-flow` —— AI 工作流编排
- 关联文档：整体架构见 `2026-08-07-ddd4j-ai-architecture-design.md`；依赖 chat/agent/memory

---

## 1. 背景

复杂 AI 任务（多步骤、条件分支、循环、人机协同）需要工作流编排。flow 组件对接 Spring AI Alibaba Graph（`spring-ai-alibaba-graph-core`），提供节点化编排能力，是 ddd4j-ai 的高阶编排层。

## 2. 目标

- 对接 Spring AI Alibaba Graph，提供工作流定义与执行端口。
- 支持节点类型：LLM 调用、工具调用、条件分支、循环、人工节点。
- 与 chat/agent/memory 集成（节点可调用各组件）。

### 非目标

- 不自研图引擎（复用 Alibaba Graph）。
- 不替代 agent 的自主编排（flow 面向预定义流程，agent 面向自主决策）。

## 3. 关键决策（建议）

| # | 决策候选 | 理由 |
|---|---------|------|
| F1 | 复用 `spring-ai-alibaba-graph-core` 2.0.0-M1.1 | 不重复造轮子 |
| F2 | 节点抽象统一（LLM/Tool/Branch/Human） | 流程可组合、可复用 |
| F3 | 流程定义支持声明式（配置/JSON） | 业务侧低门槛编排 |

## 4. 总体架构

```
业务 → FlowService（端口）→ Graph 编排引擎 → 节点（LLM/Tool/Branch/Human）
```

## 5. 接口方向

- `FlowService`：`submit(FlowDef)` → `FlowResult`；支持异步与节点回调。

## 6. 模块落点

```
ddd4j-ai-extension-flow/src/main/java/io/ddd4j/ai/cmpt/flow
├── dto/vo/ (FlowDef, FlowResult, NodeDef)
├── properties/
├── service/FlowService.java
└── service/impl/GraphFlowEngine.java
```

## 7. 依赖

- Spring AI Alibaba Graph Core 2.0.0-M1.1（`spring-ai-alibaba-graph.version`）
- ddd4j-ai-extension-chat / agent / memory

## 8. 测试策略

- 流程定义解析测试；分支/循环节点执行测试；端到端示例流程冒烟。
