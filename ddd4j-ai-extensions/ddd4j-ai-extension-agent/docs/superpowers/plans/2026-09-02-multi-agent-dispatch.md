# ddd4j-ai 多智能体派发执行（plan mode + subagents + mysql 状态 + AgentNode + xxl-job 调度）

> **实施记录（2026-09-02，全部 6 Task 完成，2.0.x 全量验证通过）**：
> - T1：`AgentProperties` 新增 `taskListEnabled`/`subagents`（SubagentSpec→SubagentDeclaration）/`stateStore`/`scheduler`；AutoConfiguration 装配 + parseSubagents 静态解析
> - T2：`MysqlAgentStateStore` 接线（`@ConditionalOnProperty(state-store=mysql)` + 业务 DataSource）；Testcontainers MySQL 实测 roundtrip/exists/delete（autoCreate 建库需 root 凭证；H2 因不支持 CREATE DATABASE 不可用）
> - T3：`AgentPlanOrchestrator`——submitPlan 建任务行 / dispatchAll 并行派发（RUNNING→DONE/FAILED 写回）/ mergeResults 父 agent synthesis；内存仓储实现 + 接口化（JDBC 后续）
> - T4：flow 组件新增 `AGENT` 节点类型（prompt 占位符→AgentService.execute→outputKey）；FlowAutoConfiguration afterName 级联 agent 装配
> - T5：`AgentScheduler` Bean 条件装配（scheduler=xxl-job + XxlJobExecutor Bean）；**agent-job 后续接入 = 新增 AgentScheduler 实现即可**（接口已抽象）
> - 执行中修正：cherry-pick 时 2.0.x 的 AgentScopeAgentAdapter 被污染（AgentEvent/限定枚举 switch），已用 1.0.x 正确版本覆盖


> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 agent/flow 两组件上落地「多智能体派发执行」四件套：
1. **AgentProperties 暴露 plan mode + subagents 配置**（PlanModeMiddleware 可选装配；subagents JSON → SubagentDeclaration）
2. **接线 agentscope-extensions-mysql**（`MysqlAgentStateStore(DataSource)` → `HarnessAgent.Builder.stateStore()` → 会话可恢复，长目标地基）
3. **orchestrator**：`AgentPlanOrchestrator`（agent 模块）——提交计划 → 任务表 → 并行派发 HarnessAgent → 结果写回 → 合并；flow 组件加 `AGENT` 节点类型（调 AgentService）
4. **调度**：暴露 Agentscope `AgentScheduler` 抽象 + `XxlJobAgentScheduler` 实现（条件装配）；后续新增 agent-job 实现只需加一个 `AgentScheduler` 实现

**已核实 API（2026-09-02，javap 于本地 jar）**：
- `HarnessAgent.Builder`：`stateStore(AgentStateStore)` / `distributedStore(DistributedStore)` / `middleware(MiddlewareBase)` / `subagents(List<SubagentDeclaration>)` / `enableTaskList()` / `workspace(Path)`
- `SubagentDeclaration.builder()`：`name/description/inlineAgentsBody/model/maxIters/steps`
- `PlanModeMiddleware(PlanModeManager, Predicate<String>)`；`PlanModeManager(WorkspaceManager, String planDir)`
- `MysqlAgentStateStore(DataSource)`（implements `io.agentscope.core.state.AgentStateStore`）；extension 自带 `H2JdbcStoreDialect`（**可用 H2 测试**）
- `AgentScheduler` 接口：`schedule(AgentConfig, ScheduleConfig) → ScheduleAgentTask` / `cancel` / `getSchedulerType`；`XxlJobAgentScheduler(XxlJobExecutor)`；传递 `xxl-job-core:3.3.2`
- `ScheduleAgentTask<T>`：`getId/getName/run(Msg...) → Mono<T>/cancel`

**Architecture**：
```
[用户/上游] submitPlan(goal, tasks[])          （AgentPlanOrchestrator）
      ↓ 写入任务表 AgentDispatchTask（PENDING）
[调度层] AgentScheduler（xxl-job / 后续 agent-job）定时触发 → dispatchAll
      ↓ 并行派发
[执行层] N × HarnessAgent.call(子任务)（多智能体，各自 subagent workspace）
      ↓ 结果写回（DONE/FAILED + result）
[合并层] 全部 DONE → mergeResults（父 agent synthesis 或直接拼接）
```

**Tech Stack:** agentscope-core/harness 2.0.0、agentscope-extensions-mysql/scheduler-common/scheduler-xxl-job 2.0.0、xxl-job-core 3.3.2（传递）、Spring Boot AutoConfigure、H2（测试）。

## Global Constraints

- `AgentService`/`AgentTask`/`AgentStep`/`AgentResult` 契约不动；既有 22 个 agent 测试全绿
- 新能力全部**条件装配**（`@ConditionalOnProperty`），默认关闭不影响既有用户
- 任务表首版内存实现（`InMemoryAgentDispatchTaskRepository`）+ 接口化（JDBC/MySQL 实现后续）
- xxl-job 仅条件装配（无 XxlJobExecutor Bean / 未配置时静默回退），不强制部署 xxl-job
- plan mode 的 `PlanModeManager(WorkspaceManager,...)` 依赖 build 后对象——采用「配置开关 + 文档说明 + PlanModeMiddleware 延迟装配」，执行时如 WorkspaceManager 无法在 build 前构造，则降级为仅暴露 `enableTaskList()` 并记录偏差
- 提交规范 conventional commits；1.0.x 先做 → cherry-pick 2.0.x（与 agent 替换批次同流程）
- 验证：`./mvnw -U -Denforcer.skip=true -B -DskipTests=false -pl ddd4j-ai-extensions/ddd4j-ai-extension-agent,ddd4j-ai-extensions/ddd4j-ai-extension-flow -am test` BUILD SUCCESS

---

### Task 1: AgentProperties 扩展 + subagents/plan-mode 装配

**Files:**
- Modify: `ddd4j-ai-extension-agent/src/main/java/io/ddd4j/ai/extension/agent/properties/AgentProperties.java`
- Modify: `autoconfigure/AgentAutoConfiguration.java`
- Test: 扩展 `AgentAutoConfigurationTest`

**Interfaces:**
- `AgentProperties` 新增：
  - `boolean taskListEnabled = false` → `builder.enableTaskList()`（内置任务清单）
  - `List<SubagentSpec> subagents = List.of()`；`SubagentSpec`（name/description/inlineAgentsBody/model/maxIters）→ `SubagentDeclaration.builder()...`
  - `String stateStore = "none"`（none/mysql）
  - `String scheduler = "none"`（none/xxl-job）
- `AutoConfiguration`：
  - harnessAgent 方法：`builder.enableTaskList(...)` + `builder.subagents(declarations)`
  - 新增静态 `parseSubagents(AgentProperties)` 供测试

- [x] Step 1 写失败测试（subagents 装配后 `HarnessAgent.getDelegate()` 可建；spec 解析正确）
- [x] Step 2 实现 + Step 3 全绿 + Step 4 Commit `feat(agent): expose subagents & task-list config (HarnessAgent multi-agent)`

---

### Task 2: agentscope-extensions-mysql 接线（AgentStateStore）

**Files:**
- Modify: agent `pom.xml`（+ `agentscope-extensions-mysql` compile）
- Modify: `AgentAutoConfiguration.java`（stateStore Bean + builder.stateStore 接线）
- Modify: `AgentProperties`（`stateStore=mysql` 时需要 `state-store-jdbc-url` 等——首版直接复用业务 DataSource：`ObjectProvider<DataSource>`）
- Test: H2 内存库验证 `MysqlAgentStateStore` 保存/读取（extension 自带 H2 方言）

**Interfaces:**
```java
@Bean
@ConditionalOnMissingBean(AgentStateStore.class)
@ConditionalOnProperty(name = "ddd4j.ai.agent.state-store", havingValue = "mysql")
public AgentStateStore agentStateStore(DataSource dataSource) {
    return new MysqlAgentStateStore(dataSource);
}
```
- harnessAgent 方法加 `ObjectProvider<AgentStateStore>` → `builder.stateStore(...)`
- 无 DataSource/未配置 → 静默不接线（回退内存态）

- [x] Step 1 依赖 + 失败测试（H2 save/get roundtrip + 装配条件）
- [x] Step 2 实现 + Step 3 全绿 + Step 4 Commit `feat(agent): wire MysqlAgentStateStore for resumable sessions`

---

### Task 3: AgentPlanOrchestrator + 任务表（多智能体派发执行核心）

**Files:**
- Create: agent 模块 `dispatch/AgentDispatchTask.java`（record：id/planId/instruction/status/result/createdAt）
- Create: `dispatch/AgentDispatchTaskRepository.java`（接口：save/findById/findByPlanId/updateResult）
- Create: `dispatch/InMemoryAgentDispatchTaskRepository.java`
- Create: `dispatch/AgentPlanOrchestrator.java`
- Modify: `AgentAutoConfiguration`（暴露 orchestrator + repository Bean）
- Test: `AgentPlanOrchestratorTest`（mock HarnessAgent）

**Interfaces:**
```java
public class AgentPlanOrchestrator {
    public AgentPlanOrchestrator(HarnessAgent harnessAgent, AgentDispatchTaskRepository repository);
    public String submitPlan(String goal, List<String> taskInstructions);   // → planId
    public Map<String, String> dispatchAll(String planId);                  // 并行派发全部 PENDING → 写回结果
    public String mergeResults(String planId);                              // 父 agent synthesis（全部 DONE 时）
}
```
- `dispatchAll`：每任务 `harnessAgent.call(new UserMessage(instruction()))` 并行（`Flux.merge`/`Mono.zip`），成功 → DONE+result，失败 → FAILED+error
- `mergeResults`：全部 DONE → `harnessAgent.call(new UserMessage(merge prompt + 各结果))`；有 FAILED → 抛 `AgentExecutionException`

- [x] Step 1 失败测试（submit 建表 / dispatch 并行写回 / merge 合成 / 部分失败抛异常 / 未知 planId）
- [x] Step 2 实现 + Step 3 全绿 + Step 4 Commit `feat(agent): add AgentPlanOrchestrator (plan → parallel subagent dispatch → merge)`

---

### Task 4: flow 组件 AGENT 节点

**Files:**
- Modify: flow `pom.xml`（+ `ddd4j-ai-extension-agent` compile）
- Modify: `service/FlowNodeType.java`（+ `AGENT`）
- Modify: `service/impl/GraphFlowService.java`（AGENT 分支 → `AgentService.execute`，结果写 outputKey）
- Test: 扩展 `GraphFlowServiceTest`（mock AgentService）

- [x] Step 1 失败测试（AGENT 节点执行：instruction 从 prompt 占位符替换，结果写 outputKey）
- [x] Step 2 实现 + Step 3 全绿 + Step 4 Commit `feat(flow): add AGENT node type delegating to AgentService`

---

### Task 5: 调度抽象 + xxl-job 实现

**Files:**
- Modify: agent `pom.xml`（+ `agentscope-extensions-scheduler-common`、`agentscope-extensions-scheduler-xxl-job` compile）
- Modify: `AgentProperties`（`scheduler=none|xxl-job`）
- Modify: `AgentAutoConfiguration`（`AgentScheduler` Bean：`@ConditionalOnProperty(scheduler=xxl-job)` + `@ConditionalOnBean(XxlJobExecutor)`）
- Test: 装配测试（mock XxlJobExecutor）

**Interfaces:**
```java
@Bean
@ConditionalOnMissingBean(AgentScheduler.class)
@ConditionalOnProperty(name = "ddd4j.ai.agent.scheduler", havingValue = "xxl-job")
public AgentScheduler agentScheduler(ObjectProvider<XxlJobExecutor> executorProvider) { ... }
```
- 后续 agent-job：新增 `AgentJobAgentScheduler implements AgentScheduler` 即插入（接口已抽象）
- **后续任务（本 plan 外）**：`/Users/wandl/workspaces/workspace-octoclaw-labs/agent-job` 与 agentscope 整合 = 新增 extensions-scheduler-agent-job

- [x] Step 1 失败测试（scheduler=xxl-job + XxlJobExecutor Bean → AgentScheduler 装配；未配置回退）
- [x] Step 2 实现 + Step 3 全绿 + Step 4 Commit `feat(agent): expose AgentScheduler with xxl-job implementation (agent-job pluggable)`

---

### Task 6: 全量验证 + docs 回写 + 双分支

- [x] Step 1 全量 reactor `clean test` BUILD SUCCESS
- [x] Step 2 plan 勾选 + 实施记录；agent spec 状态更新
- [x] Step 3 推送 feature/1.0.x 双远程
- [x] Step 4 cherry-pick → feature/2.0.x（pom modelVersion 冲突按 4.1.0 解决）→ 全量验证 → 推送双远程

---

## Self-Review

- **多智能体派发执行**：Task 3 核心覆盖（submit→表→并行派发→写回→merge）；Task 1 subagents 提供声明式子智能体；Task 5 调度触发
- **长目标**：Task 2 mysql 状态持久化（会话可恢复）+ Task 5 调度（定时续跑）——外部循环驱动，不靠无限对话
- **契约零破坏**：全部条件装配，默认关闭；既有 22 agent 测试不动
- **agent-job 后续接入**：面向 `AgentScheduler` 接口编程，新增实现类即可（Task 5 明确记录）
- **依赖新增**：agentscope-extensions-mysql/scheduler-common/scheduler-xxl-job（BOM 2.0.2 已管理，无版本号引入）；xxl-job-core 3.3.2 传递
- **占位扫描**：无 TODO；PlanModeMiddleware 的 WorkspaceManager 时序问题已标注降级策略
