# Chat 对话组件设计

- 日期：2026-08-12
- 作者：PartMe.AI
- 状态：**已实现**（2026-08-16，v1.x-A）
- 范围：`ddd4j-ai-extension-chat` —— 基于 Spring AI ChatClient 的对话能力封装
- 关联文档：整体架构见 `2026-08-07-ddd4j-ai-architecture-design.md`；core 契约见 `2026-07-01-ai-core-contract-design.md`；与 memory 集成见 `2026-08-12-memory-component-design.md`

---

## 1. 背景

对话是 LLM 应用最基础的能力。chat 组件在 Spring AI `ChatClient` 之上做 COLA 封装，对接 core 的 `AiHandler` 契约，使业务服务以统一端口调用大模型，并为 memory（多轮）、rag（检索增强）、agent（工具调用）提供底座。

## 2. 目标

- 封装 Spring AI `ChatClient`，对外暴露端口接口（适配 core `AiHandler`）。
- 支持单轮对话与多轮对话（依赖 memory 组件）。
- 预留流式响应扩展点。
- 通过 `ddd4j-ai-dependencies` 统一治理 Spring AI 2.0.0 版本。

### 非目标

- 不内置具体模型供应商绑定（由业务侧 Spring AI autoconfigure 提供 API Key）。
- 不实现检索增强（由 rag 组件承担）。
- 不实现工具调用编排（由 agent + mcp 承担）。

## 3. 关键决策（建议，待实施时确认）

| # | 决策候选 | 理由 |
|---|---------|------|
| C1 | 端口接口实现 `AiHandler`，`handle(AiRequest)` 走 ChatClient | 复用 core 统一调用契约 |
| C2 | 多轮上下文通过注入 `ChatMemory`（memory 组件）实现 | 不在 chat 内自建会话存储 |
| C3 | 流式响应提供独立方法返回 `Flux`/`Publisher` | 与同步 `handle` 分离，不破坏 core 契约 |

## 4. 总体架构

```
业务应用层 → ChatService（端口，extends AiHandler 或独立端口）
                └── impl/ChatClientAdapter → Spring AI ChatClient
```

## 5. 接口方向（待实施时定稿）

- `ChatService`：单轮 `chat(String)` → `String`；多轮 `chat(String, conversationId)`（接 memory）；流式 `streamChat(String)`。
- 可选：实现 core `AiHandler.handle(AiRequest)`，input 即 prompt。

## 6. 模块落点

```
ddd4j-ai-extension-chat/src/main/java/io/ddd4j/ai/cmpt/chat
├── dto/ vo/ enums/
├── properties/   (ChatProperties: 默认模型/参数)
├── service/ChatService.java
└── service/impl/ChatClientAdapter.java
```

## 7. 依赖

- Spring AI 2.0.0（`spring-ai-bom`，ChatClient）
- ddd4j-ai-core（AiHandler 契约）
- 可选：ddd4j-ai-extension-memory（多轮）

## 8. 测试策略

- 端口契约测试；ChatClient mock 验证 prompt 构造与响应映射；流式分段校验。
