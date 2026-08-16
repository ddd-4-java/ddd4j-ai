# RAG 检索增强生成组件设计

- 日期：2026-08-12
- 作者：PartMe.AI
- 状态：**已实现**（2026-08-16，v1.x-B 提前交付 RAG 部分）
- 范围：`ddd4j-ai-extension-rag` —— 检索增强生成管道
- 关联文档：整体架构见 `2026-08-07-ddd4j-ai-architecture-design.md`；依赖 chat `2026-08-12-chat-component-design.md` / embedding `2026-08-12-embedding-component-design.md` / vectordb `2026-08-12-vectordb-component-design.md`

---

## 1. 背景

RAG（Retrieval-Augmented Generation）是企业 LLM 落地最高频场景。rag 组件编排"检索 → 增强 → 生成"管道，串联 chat + embedding + vectordb，提供开箱即用的知识问答能力。

## 2. 目标

- 提供端到端 RAG 端口：问题 → 检索相关片段 → 构造增强 prompt → 调用 chat 生成。
- 支持文档摄取管道（加载 → 分块 → 嵌入 → 入库）。
- 支持检索质量优化：重排序（rerank）、metadata 过滤、Top-K 调参。

### 非目标

- 不重新实现嵌入/向量库/对话（复用对应组件）。
- 不内置具体 rerank 模型（提供扩展点）。

## 3. 关键决策（建议）

| # | 决策候选 | 理由 |
|---|---------|------|
| R1 | 管道以组合方式编排 chat/embedding/vectordb 端口 | 复用已建组件，单一职责 |
| R2 | prompt 模板可配置（系统提示 + 检索上下文槽） | 不同业务知识库语料差异大 |
| R3 | 提供 rerank 扩展点（默认空实现/直通） | 质量与成本可调 |

## 4. 总体架构

```
业务 → RagService（端口）
         ├── ingest: DocumentReader → Splitter → EmbeddingService → VectorDbService
         └── query:  问题 → VectorDbService.search → [rerank] → prompt 拼装 → ChatService
```

## 5. 接口方向

- `RagService`：`ingest(List<Document>)`；`query(String question)` → `String`；`queryStream(...)`。

## 6. 模块落点

```
ddd4j-ai-extension-rag/src/main/java/io/ddd4j/ai/cmpt/rag
├── properties/ (RagProperties: topK/模板/rerank 开关)
├── service/RagService.java
└── service/impl/RagPipeline.java
```

## 7. 依赖

- ddd4j-ai-extension-chat / embedding / vectordb（组合）
- Spring AI 2.0.0（DocumentReader/Splitter、prompt 模板）

## 8. 测试策略

- 端到端 RAG 集成测试（mock 三组件）；prompt 拼装单测；rerank 扩展点可替换性测试。
