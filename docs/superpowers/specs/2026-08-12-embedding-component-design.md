# Embedding 向量嵌入组件设计

- 日期：2026-08-12
- 作者：PartMe.AI
- 状态：待实施（骨架：仅 package-info 占位）
- 范围：`ddd4j-ai-extension-embedding` —— 文本向量嵌入
- 关联文档：整体架构见 `2026-08-07-ddd4j-ai-architecture-design.md`；被 rag/vectordb 引用

---

## 1. 背景

向量嵌入是检索增强（RAG）与语义搜索的基础。embedding 组件封装 Spring AI `EmbeddingModel`，提供批量嵌入与多供应商切换，为 vectordb 入库与 rag 检索提供统一向量化入口。

## 2. 目标

- 封装 Spring AI `EmbeddingModel`，提供单条与批量嵌入端口。
- 支持多供应商切换（OpenAI、本地模型、千帆/Moonshot 等社区实现）。
- 与 vectordb 组件协同（嵌入结果直接入库）。

### 非目标

- 不实现向量存储（由 vectordb 承担）。
- 不实现检索排序（由 rag 承担）。

## 3. 关键决策（建议）

| # | 决策候选 | 理由 |
|---|---------|------|
| E1 | 对接 Spring AI `EmbeddingModel` | 复用生态，供应商无关 |
| E2 | 批量嵌入接口，控制单批大小 | 避免供应商限流与超时 |
| E3 | 维度/模型名透传至 metadata | 便于 vectordb 集合按维度分库 |

## 4. 总体架构

```
rag/vectordb → EmbeddingService（端口）→ impl/EmbeddingModelAdapter → Spring AI EmbeddingModel
```

## 5. 接口方向

- `EmbeddingService`：`embed(String)` → `float[]`；`embedBatch(List<String>)` → `List<float[]>`。

## 6. 模块落点

```
ddd4j-ai-extension-embedding/src/main/java/io/ddd4j/ai/cmpt/embedding
├── properties/ (EmbeddingProperties: 模型名/批量大小)
├── service/EmbeddingService.java
└── service/impl/EmbeddingModelAdapter.java
```

## 7. 依赖

- Spring AI 2.0.0（`EmbeddingModel`）
- 可选：Spring AI Community（千帆/Moonshot）

## 8. 测试策略

- 端口契约测试；批量分片与限流重试单测；维度一致性校验。
