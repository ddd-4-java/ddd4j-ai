# v1.x-A LLM 基础层（chat / memory / embedding / vectordb）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> **Completed 2026-08-16**：四组件全部落地（统一 COLA 结构 + 标准 @AutoConfiguration，配置前缀 `ddd4j.ai.<component>.*`）。单元/契约/装配测试全绿；Testcontainers 集成测试采用 Ollama（qwen2.5:0.5b + all-minilm）、PostgreSQL、Redis Stack（RediSearch）、pgvector 四类镜像，`disabledWithoutDocker` 自动跳过。实施修订：① memory 合并为单实现 `WindowMemoryService`（repository 可插拔，官方 jdbc/redis starter 注入即用）；② vectordb 以通用 `VectorStoreAdapter` 替代三后端适配器；③ chat→memory 为普通编译依赖（避免 optional 依赖的类加载陷阱，memory 默认实现零成本）。

**Goal:** 实现 chat（对话）、memory（记忆）、embedding（向量嵌入）、vectordb（向量数据库）四个基础 LLM 组件，为 RAG 与 Agent 提供底层能力。

**Architecture:** 四模块均遵循 COLA 分层（见架构 spec 第 8 节），通过 Spring AI 抽象层（`ChatClient`/`ChatMemory`/`EmbeddingModel`/`VectorStore`）隔离供应商，对外暴露端口接口；业务侧经 `ddd4j-ai-bom` 按需引入。

**Tech Stack:** Java 17、Maven、Spring AI 2.0.0、Spring Boot AutoConfiguration、JUnit 5 + Mockito + Testcontainers。

**Related Design Doc:** `docs/superpowers/specs/2026-08-12-chat-component-design.md`、`2026-08-12-memory-component-design.md`、`2026-08-12-embedding-component-design.md`、`2026-08-12-vectordb-component-design.md`；架构基线 `2026-08-07-ddd4j-ai-architecture-design.md`。

## Global Constraints

- 不破坏 core 契约；新组件端口优先适配/复用 `AiHandler`。
- 每组件独立模块、独立 AutoConfiguration，按需引入不互相强依赖（memory 为 chat 可选依赖）。
- TDD：先写可观察的失败测试，再实现。
- 提交约定：conventional commits（`feat(chat): ...`）。
- 不在本批引入具体模型供应商 starter（由业务侧提供 Key）。

---

## Task 1: 实现 ddd4j-ai-extension-chat

**Files:**
- Create: `.../cmpt/chat/service/ChatService.java`、`service/impl/ChatClientAdapter.java`、`properties/ChatProperties.java`、`autoconfigure/ChatAutoConfiguration.java`
- Test: `.../cmpt/chat/service/ChatServiceContractTest.java`、`impl/ChatClientAdapterTest.java`

**Interfaces:** Produces ChatService；Consumes ddd4j-ai-core、Spring AI ChatClient

- [x] **Step 1:** 写失败测试 —— `ChatService.chat(String)` 经 mock ChatClient 返回期望文本；多轮 `chat(String, conversationId)` 注入 memory。
- [x] **Step 2:** 定义 ChatService 端口（单轮/多轮/流式扩展点）。
- [x] **Step 3:** 实现 ChatClientAdapter 适配 Spring AI ChatClient；可选实现 core `AiHandler`。
- [x] **Step 4:** ChatProperties + AutoConfiguration。
- [x] **Step 5:** 绿后 `git commit -m "feat(chat): 实现对话组件基础能力"`。

---

## Task 2: 实现 ddd4j-ai-extension-memory

**Files:**
- Create: `.../cmpt/memory/service/MemoryService.java`、`service/impl/{InMemory,Redis,Jdbc}MemoryService.java`、`properties/MemoryProperties.java`
- Test: `.../cmpt/memory/service/MemoryServiceContractTest.java`、`impl/{InMemory,Redis}MemoryServiceTest.java`

**Interfaces:** Produces MemoryService；Consumes Spring AI ChatMemory

- [x] **Step 1:** 写失败测试 —— add/get/clear 契约；窗口截断 N 轮；摘要触发阈值。
- [x] **Step 2:** 定义 MemoryService 端口，适配 Spring AI `ChatMemory`。
- [x] **Step 3:** 实现 InMemory（默认）/ Redis / Jdbc 多后端。
- [x] **Step 4:** MemoryProperties（后端类型/窗口/阈值）+ 按后端条件装配。
- [x] **Step 5:** Redis/Jdbc 用 Testcontainers 集成测试。
- [x] **Step 6:** 绿后 `git commit -m "feat(memory): 实现会话记忆多后端组件"`。

---

## Task 3: 实现 ddd4j-ai-extension-embedding

**Files:**
- Create: `.../cmpt/embedding/service/EmbeddingService.java`、`service/impl/EmbeddingModelAdapter.java`、`properties/EmbeddingProperties.java`
- Test: `.../cmpt/embedding/service/EmbeddingServiceContractTest.java`、`impl/EmbeddingModelAdapterTest.java`

**Interfaces:** Produces EmbeddingService；Consumes Spring AI EmbeddingModel

- [x] **Step 1:** 写失败测试 —— 单条/批量嵌入维度一致；批量分片不超批量上限；限流重试。
- [x] **Step 2:** 定义 EmbeddingService 端口（`embed`/`embedBatch`）。
- [x] **Step 3:** 实现 EmbeddingModelAdapter；metadata 透传模型名/维度。
- [x] **Step 4:** 绿后 `git commit -m "feat(embedding): 实现向量嵌入组件"`。

---

## Task 4: 实现 ddd4j-ai-extension-vectordb

**Files:**
- Create: `.../cmpt/vectordb/service/VectorDbService.java`、`service/impl/{Milvus,PgVector,Redis}VectorStoreAdapter.java`、`properties/VectorDbProperties.java`
- Test: `.../cmpt/vectordb/service/VectorDbServiceContractTest.java`、`impl/VectorStoreAdapterIT.java`

**Interfaces:** Produces VectorDbService；Consumes Spring AI VectorStore、embedding

- [x] **Step 1:** 写失败测试 —— add/delete/search 契约；metadata 过滤；分块入库。
- [x] **Step 2:** 定义 VectorDbService 端口。
- [x] **Step 3:** 实现多后端 Adapter；分块策略可插拔（字符/Token/递归）。
- [x] **Step 4:** 多后端用 Testcontainers 冒烟。
- [x] **Step 5:** 绿后 `git commit -m "feat(vectordb): 实现向量库多后端组件"`。

---

## Task 5: 集成验证（chat + memory + embedding + vectordb）

**Files:**
- Test: `ddd4j-ai-samples/src/test/.../LlmFoundationSmokeIT.java`

- [x] **Step 1:** 写联合冒烟：多轮对话经 memory 保持上下文；嵌入入库后可检索。
- [x] **Step 2:** 绿后 `git commit -m "test(samples): LLM 基础层联合冒烟"`。

---

## Task 6: 补齐本批各组件 sample 示例

**Files:**
- Create: `ddd4j-ai-samples/.../samples/{chat,memory,embedding,vectordb}/...`

- [x] **Step 1:** 每组件一个最小可运行示例（配置 + 注入 + 调用）。
- [x] **Step 2:** `git commit -m "docs(samples): 补充 chat/memory/embedding/vectordb 示例"`。

---

## Self-Review 结论

- **Spec coverage:** Task 1–4 一一对应四份组件 spec；Task 5 覆盖集成；Task 6 覆盖 samples。✅
- **Placeholder scan:** 无 TODO/TBD，待实施项均落 Step。✅
- **Type consistency:** 端口命名后缀统一（`*Service`），impl 后缀统一（`*Adapter`/`*Service`）。✅
