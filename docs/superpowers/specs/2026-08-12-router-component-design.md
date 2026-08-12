# Router 多模型智能路由组件设计

- 日期：2026-08-12
- 作者：PartMe.AI
- 状态：待实施（骨架：仅 package-info 占位）
- 范围：`ddd4j-ai-extension-router` —— 多模型智能路由与负载策略
- 关联文档：整体架构见 `2026-08-07-ddd4j-ai-architecture-design.md`；依赖 chat

---

## 1. 背景

生产环境常需多模型（不同供应商/不同规格）协同：按成本、延迟、能力、配额路由请求。router 组件提供多模型路由与负载均衡，是规模化 LLM 应用的治理层。

## 2. 目标

- 提供多模型路由策略：轮询、权重、成本优先、延迟优先、能力匹配。
- 提供负载均衡与故障转移（主模型失败自动切备）。
- 与 chat 组件集成（router 选择模型后交 chat 执行）。

### 非目标

- 不实现模型客户端（由 chat + Spring AI 提供）。
- 不实现缓存（可后续独立组件）。

## 3. 关键决策（建议）

| # | 决策候选 | 理由 |
|---|---------|------|
| RR1 | 路由策略接口可插拔 | 不同业务策略不同 |
| RR2 | 健康检查 + 故障转移 | 高可用 |
| RR3 | 指标采集（成本/延迟/成功率） | 支撑策略调优 |

## 4. 总体架构

```
业务 → RouterService（端口）→ 路由策略 → 选择模型 → ChatService 执行
                                ↑ 健康检查/指标
```

## 5. 接口方向

- `RouterService`：`route(AiRequest)` → 选定模型 ChatService；`register(ModelSpec)`。

## 6. 模块落点

```
ddd4j-ai-extension-router/src/main/java/io/ddd4j/ai/cmpt/router
├── dto/vo/enums/ (ModelSpec, RoutePolicy)
├── properties/ (RouterProperties: 策略/权重/熔断)
├── service/RouterService.java
└── service/impl/{RoundRobin, Weighted, Cost, Latency}Router.java
```

## 7. 依赖

- ddd4j-ai-extension-chat
- Spring AI 2.0.0

## 8. 测试策略

- 各路由策略单测；故障转移回路测试；并发下权重分布校验。
