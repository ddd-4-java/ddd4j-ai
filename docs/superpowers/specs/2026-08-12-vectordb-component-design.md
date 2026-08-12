# VectorDB 向量数据库组件设计

- 日期：2026-08-12
- 作者：PartMe.AI
- 状态：待实施（骨架：仅 package-info 占位）
- 范围：`ddd4j-ai-extension-vectordb` —— 向量数据库接入与检索
- 关联文档：整体架构见 `2026-08-07-ddd4j-ai-architecture-design.md`；嵌入见 `2026-08-12-embedding-component-design.md`；被 rag 引用

---

## 1. 背景

向量库是 RAG 的持久化与检索后端。vectordb 组件封装 Spring AI `VectorStore` 抽象，提供文档入库（含分块）与相似度检索端口，支持多后端切换，使 rag 组件与具体向量库解耦。

## 2. 目标

- 封装 Spring AI `VectorStore`，提供 add/delete/search 端口。
- 提供文档加载与分块（chunk）策略。
- 支持多后端：Milvus、PgVector、Redis Vector 等。

### 非目标

- 不实现嵌入计算（由 embedding 承担）。
- 不实现检索增强编排（由 rag 承担）。

## 3. 关键决策（建议）

| # | 决策候选 | 理由 |
|---|---------|------|
| V1 | 对接 Spring AI `VectorStore` | 多后端统一抽象 |
| V2 | 分块策略可插拔（按字符/Token/递归） | 适应不同文档类型 |
| V3 | 检索支持 metadata 过滤 | 支持权限/来源筛选 |

## 4. 总体架构

```
rag → VectorDbService（端口）→ impl/{Milvus,PgVector,Redis}VectorStoreAdapter → Spring AI VectorStore
```

## 5. 接口方向

- `VectorDbService`：`add(List<Document>)`、`delete(List<String> ids)`、`search(String query, int topK, Filter)`。

## 6. 模块落点

```
ddd4j-ai-extension-vectordb/src/main/java/io/ddd4j/ai/cmpt/vectordb
├── properties/ (VectorDbProperties: 后端类型/分块大小)
├── service/VectorDbService.java
└── service/impl/{Milvus,PgVector,Redis}VectorStoreAdapter.java
```

## 7. 依赖

- Spring AI 2.0.0（`VectorStore`、`DocumentReader/Splitter`）
- 可选：Milvus / PgVector / Redis 客户端（按后端）

## 8. 测试策略

- 端口契约测试；分块策略单测；多后端用 Testcontainers 做集成冒烟。
