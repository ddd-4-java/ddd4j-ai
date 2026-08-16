# ddd4j-ai 运行时版本线对齐设计（boot 4.x / FW 7 / Spring AI 2.0 完整启用）

- 日期：2026-08-16
- 作者：PartMe.AI
- 状态：已确认（当日实施）
- 范围：`ddd4j-ai` 父链版本对齐 + Spring Framework 7 覆盖策略 + Spring AI 2.0 真实模型链路启用
- 关联文档：`2026-08-07-ddd4j-ai-architecture-design.md` §9.4（兼容性约束来源）

---

## 1. 背景

v1.x-A/B 实施中发现：Spring AI 2.0 的模型客户端（`OllamaApi` 等）运行时依赖 Spring Framework 7
新增的 `org.springframework.core.retry` 包；ddd4j-ai 当时父链为
`ddd4j-parent:2.0.x` + `ddd4j-boot-dependencies:3.4.x`（Boot 3.5 / FW 6.2），不含该包，
导致 Ollama 真实模型集成测试只能按 classpath 探测跳过。

项目版本线对应关系（用户提供，2026-08-16）：

| ddd4j-boot 版本线 | release | 对应 ddd4j 线 | Spring 生态 |
|---|---|---|---|
| 3.4.x / 3.5.x | 3.4.13 / 3.5.16 | feature/2.0.x | Boot 3.5 / FW 6.2（当前 ddd4j-ai 所在） |
| 4.0.x | 4.0.7 | feature/3.0.x | Boot 4.0.5（starter-parent） |
| 4.1.x | 4.1.0 | feature/3.0.x | （本地分支暂缺） |

## 2. 关键事实（探查核实）

1. **boot 4.0.x 分支**（本地 `ddd4j-boot` 仓库，revision `4.0.x.20251215-SNAPSHOT`）：
   根 pom 继承 `spring-boot-starter-parent:4.0.5`，`spring-framework.version=7.0.8`；
   **但 `ddd4j-boot-dependencies` 模块存在显式约束注释「Spring Framework 锁定 6.x，不得升级至 7.x」
   并管理 `spring-framework.version=6.2.19`**。
2. 私仓中 `4.0.x.20251215-SNAPSHOT` 的 SNAPSHOT 文件已被清理（metadata 在、构件 404），
   无法直接远程解析——需本地构建安装。
3. `ddd4j` 仓库 `feature/3.0.x` 分支本地已检出，revision `3.0.x.20260630-SNAPSHOT`（与 boot 3.4.x 同日期线）。

## 3. 结论：仅对齐 boot 4.0.x 不足以启用 Spring AI 2.0

boot 4.0.x 的 dependencies 模块把 FW 压回 6.2.19。因此本设计采用**两级动作**：

- **A. 版本线对齐**：ddd4j-ai 父链升级至 `ddd4j-parent:3.0.x` + `ddd4j-boot-dependencies:4.0.x`
  （跟上 Boot 4 生态与 ddd4j 3.0.x 契约）。
- **B. FW7 定向覆盖**：在 `ddd4j-ai-dependencies` 的 `dependencyManagement` 中，
  **在 ddd4j-boot-dependencies 之前** import `spring-framework-bom:7.0.8`
  （Maven 规则：同 pom 内先 import 的 BOM 条目优先），使 spring-core/context/beans/web 等全套对齐 FW 7.0.8，
  满足 Spring AI 2.0 运行时要求。

## 4. 取舍与风险

| 风险 | 评估 | 对策 |
|---|---|---|
| 覆盖 boot 4.0.x 的 FW 锁定（违规"不得升级至 7.x"注释） | ddd4j-ai 拥有独立 dependencies 治理域，覆盖是本模块设计初衷；但与 boot 线全局策略冲突 | 在本 spec 与架构 spec §9.4 显式记录；待 boot 4.1.x 线放开 FW7 后回收覆盖 |
| FW 7 + Boot 4 生态其余第三方（springdoc 2.7 等）兼容 | ddd4j-ai 仅用 spring-core/context/beans/jdbc + boot autoconfigure/test，面窄 | 全量 94 测试 + 容器 IT 作为回归安全网 |
| 本地构建 boot 4.0.x / ddd4j 3.0.x 引入未发布 SNAPSHOT | 版本号与私仓 metadata 一致（4.0.x.20251215-SNAPSHOT / 3.0.x.20260630-SNAPSHOT），后续私仓恢复可无缝替换 | 本地 install 供当前开发；发布前可重推私仓 |
| Boot 3.5 → 4.0 的 API 变化 | sst 的 `@ConfigurationProperties` 绑定、测试栈（JUnit/Mockito 由 ddd4j-dependencies 管理）可能版本跳动 | 逐模块修复，测试验证 |

## 5. 验证标准

1. 全项目 `mvn test` 全绿（含 sst 38 个存量测试不回归）。
2. Ollama 集成测试的 classpath 探测跳过**解除后真实执行**：
   qwen2.5:0.5b 对话链路、all-minilm 嵌入链路、RAG 联合冒烟全绿。
3. 架构 spec §9.4 由"已知约束"改写为"已解决（方案与生效版本记录）"。
