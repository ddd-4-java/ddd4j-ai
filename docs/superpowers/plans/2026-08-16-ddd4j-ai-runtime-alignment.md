# ddd4j-ai 运行时对齐（boot 4.0.x / FW 7 / 启用 Spring AI 2.0 真实模型链路）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 ddd4j-ai 父链从 boot 3.4.x（FW 6.2）对齐至 boot 4.0.x / ddd4j 3.0.x，并通过前置 spring-framework-bom 覆盖启用 Spring AI 2.0 完整运行时，跑通 Ollama 真实模型集成测试。

**Architecture:** 两级动作：A) 父链版本升级（`ddd4j-parent:3.0.x.20260630-SNAPSHOT` + `ddd4j-boot-dependencies:4.0.x.20251215-SNAPSHOT`，均需本地构建安装，私仓 SNAPSHOT 已失效）；B) 在 `ddd4j-ai-dependencies` 将 `spring-framework-bom:7.0.8` 置于 boot-dependencies 之前 import，覆盖其 FW 6.2.19 锁定（见 `2026-08-16-runtime-alignment-design.md` §3-§4）。

**Tech Stack:** Maven（git worktree 隔离构建上游 SNAPSHOT）、Spring Boot 4.0.5 / Spring Framework 7.0.8、Spring AI 2.0.0、Testcontainers（Ollama/pgvector/postgres/redis-stack）。

**Related Design Doc:** `docs/superpowers/specs/2026-08-16-runtime-alignment-design.md`

## Global Constraints

- 不改动 ddd4j-boot / ddd4j 两个仓库的用户工作区（分支/文件），上游构建一律走 `git worktree`（boot 4.0.x）或只读 install（ddd4j feature/3.0.x 已检出）。
- 94 个存量测试必须全绿才算升级成功；Ollama 探测跳过逻辑保留为"环境自愈"能力（FW 缺失时仍优雅跳过）。
- 提交约定：conventional commits。

---

## Task 1: 本地构建上游 SNAPSHOT（boot 4.0.x + ddd4j 3.0.x）

- [x] **Step 1:** `ddd4j-boot` 仓库 `git worktree add /tmp/ddd4j-boot-4.0.x 4.0.x`，构建安装 `ddd4j-boot-dependencies`（含父链，skipTests）。
- [x] **Step 2:** `ddd4j` 仓库（feature/3.0.x 已检出）安装 `ddd4j-parent` 及其父链（ddd4j 根 + ddd4j-dependencies，skipTests）。

## Task 2: ddd4j-ai 父链升级 + FW7 覆盖

- [x] **Step 1:** 根 pom：`ddd4j-parent` 2.0.x.20260630-SNAPSHOT → 3.0.x.20260630-SNAPSHOT。
- [x] **Step 2:** `ddd4j-ai-dependencies`：`ddd4j-boot.version` → 4.0.x.20251215-SNAPSHOT；新增 `spring-framework-bom:7.0.8` **前置** import（位于 boot-dependencies 之前）。
- [x] **Step 3:** 提交本 Task（版本对齐独立成笔）。

## Task 3: 兼容性修复与全量回归

- [x] **Step 1:** `mvn test` 全量回归，修复 Boot 3.5→4.0 / 测试栈版本跳动引入的编译或测试问题。
- [x] **Step 2:** 确认 sst 38 个存量测试零回归。

## Task 4: 启用 Ollama 真实模型链路

- [x] **Step 1:** FW7 生效后 classpath 探测通过，Ollama 三组集成测试真实执行（qwen2.5:0.5b + all-minilm）。
- [x] **Step 2:** RAG 联合冒烟（Ollama × pgvector）全绿。

## Task 5: 文档回写与发布

- [x] **Step 1:** 架构 spec §9.4 改写为"已解决"并记录生效版本；版本表 §9.2 补充 FW/Boot 对齐记录。
- [x] **Step 2:** 提交推送双远程（codeup + ddd-4-java）。

## Self-Review 结论

- **Spec coverage:** Task 1-2 ↔ spec §3 动作 A/B；Task 3-4 ↔ spec §5 验证标准；Task 5 ↔ 文档回写。✅
- **Placeholder scan:** 无 TODO/TBD。✅
- **Type consistency:** SNAPSHOT 版本号与私仓 metadata 一致，后续可无缝替换。✅
