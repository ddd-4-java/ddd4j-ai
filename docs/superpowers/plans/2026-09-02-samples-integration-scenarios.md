# ddd4j-ai-samples 集成示例完善计划

## 目标

从"每个组件一个独立 Sample"升级为"7 个集成场景 + 13 个基础组件示例共存"，每个场景串联多个组件，展示真实业务能力。

## 7 个集成场景

| # | 目录 | 组件串联 | 关键方法 |
|---|------|---------|---------|
| 1 | `chat-memory-sse/` | chat + memory | `multiTurn()`/`streamFlux()` |
| 2 | `rag-pipeline/` | document + embedding + vectordb + rag + chat | `ingestDocument()`/`askWithRag()` |
| 3 | `smart-agent/` | agent + document + rag + mcp | `run()`/`runWithKnowledge()` |
| 4 | `knowledge-base/` | document + embedding + vectordb + rag | `ingestFile()`/`search()` |
| 5 | `multi-model-router/` | router + chat | `route()`/`routeStream()` |
| 6 | `doc-understanding/` | document + ocr + chat + agent | `askAboutPdf()`/`extractAndAsk()` |
| 7 | `orchestration/` | flow + agent + router | `runSequentialFlow()`/`runAgentNode()` |

## 约束

- samples pom 补 document 依赖
- 每个 Sample 是 `@Component`，通过构造器注入多个端口接口
- SSE 流式由方法返回 `Flux<String>`（HTTP 端点由 sample-app 或用户业务代码提供）
- 不引入新第三方依赖
- 提交到 feature/2.0.x → cherry-pick 1.0.x

## 验证

- `./mvnw -pl ddd4j-ai-samples compile` 编译通过
- 全量 reactor `clean test` BUILD SUCCESS
