# Memory 会话记忆组件设计

- 日期：2026-08-12
- 作者：PartMe.AI
- 状态：待实施（骨架：仅 package-info 占位）
- 范围：`ddd4j-ai-extension-memory` —— 会话上下文与记忆管理
- 关联文档：整体架构见 `2026-08-07-ddd4j-ai-architecture-design.md`；被 chat/agent 引用

---

## 1. 背景

多轮对话、Agent 规划均依赖会话记忆。memory 组件对接 Spring AI `ChatMemory` 抽象，提供窗口记忆与摘要记忆策略，并支持多后端存储，使 chat/agent 组件以统一方式存取上下文。

## 2. 目标

- 提供会话记忆端口（基于 Spring AI `ChatMemory`）。
- 支持窗口策略（保留最近 N 轮）与摘要策略（滚动摘要历史）。
- 支持多后端：内存（开发期）、Redis、持久化 DB。

### 非目标

- 不实现向量检索（由 vectordb 承担）。
- 不绑定具体对话语义（由 chat 组件解释）。

## 3. 关键决策（建议）

| # | 决策候选 | 理由 |
|---|---------|------|
| M1 | 对接 Spring AI `ChatMemory` 而非自建 | 复用生态，与 ChatClient 天然集成 |
| M2 | 端口 + 多 impl（InMemory/Redis/Jdbc） | 开发期内存、生产期 Redis/DB |
| M3 | 策略可插拔（窗口 N、摘要触发阈值） | 不同场景记忆成本/质量权衡 |

## 4. 总体架构

```
chat/agent → MemoryService（端口）→ impl/{InMemory,Redis,Jdbc}MemoryService
```

## 5. 接口方向

- `MemoryService`：`add(conversationId, Message)`、`get(conversationId)`、`clear(conversationId)`。
- 适配 Spring AI `ChatMemory` 接口。

## 6. 模块落点

```
ddd4j-ai-extension-memory/src/main/java/io/ddd4j/ai/cmpt/memory
├── properties/ (MemoryProperties: 后端类型/窗口大小/摘要阈值)
├── service/MemoryService.java
└── service/impl/{InMemory,Redis,Jdbc}MemoryService.java
```

## 7. 依赖

- Spring AI 2.0.0（`ChatMemory`）
- 可选：Spring Data Redis / Jdbc（按后端）

## 8. 测试策略

- 端口契约测试覆盖 add/get/clear；窗口截断与摘要触发逻辑单测；多后端用 Testcontainers。
