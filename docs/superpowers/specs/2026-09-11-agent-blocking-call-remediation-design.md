# Agent 阻塞调用治理设计（reactive 入口 + Flow 路径修复）

- 日期：2026-09-11
- 作者：PartMe.AI
- 状态：待实施（方案已选定：方案 A）
- 范围：`ddd4j-ai-extension-agent`（`AgentService` / `AgentScopeAgentAdapter` / `AgentPlanOrchestrator`）+ `ddd4j-ai-extension-flow`（`GraphFlowService`）
- 分支：`feature/2.0.x`（实施与验证）+ `feature/1.0.x`（同补丁移植，前提是该线可构建——见第 9 节残留项）
- 关联：本文件是「ddd4j-ai 优化改造」6 项序列中的**第 2 项**；第 1 项见 `2026-09-11-agent-fine-grained-event-stream-design.md`

---

## 1. 背景

`AgentService.execute` 与 `AgentPlanOrchestrator.dispatchAll` / `mergeResults` 是**同步 API**，内部以 `.block()` 收口。当调用方在 Reactor NonBlocking 线程（WebFlux/Netty、`Schedulers.parallel()`、`Schedulers.single()` 等）上使用时，`.block()` 会抛 `IllegalStateException`。

`GraphFlowService.agentAction`（AGENT 节点）在 `AsyncNodeAction` 里调 `agentService.execute(...)`，是仓内**唯一**能走到该路径的生产代码。

## 2. 关键事实（全部实测，含两次推翻早期推断）

### F1. `.block()` 调用点清单（生产代码，共 4 处）

| # | 位置 | 所在方法 | 危险来源 |
|---|------|---------|---------|
| 1 | `AgentScopeAgentAdapter:47` | `execute` | 调用方线程 |
| 2 | `AgentPlanOrchestrator:83` | `dispatchAll` 末端 | 调用方线程 |
| 3 | `AgentPlanOrchestrator:118` | `mergeResults` 末端 | 调用方线程 |
| 4 | `AgentPlanOrchestrator:123` | `executeTask` 内层（`Mono.fromCallable` 内 `.block()`）| 订阅线程 |

### F2. 触发条件（三组对照实验取证）

探针：`GraphFlowService` + **真实** `AgentScopeAgentAdapter`（仅 mock `HarnessAgent`），给 agent 调用注入 300ms 延迟，并记录线程名。

| 场景 | 节点动作线程 | 结果 |
|------|-------------|------|
| 调用方线程订阅（`main`）| `main`（isNonBlocking=false）| ✅ 成功 |
| 非阻塞线程订阅 + **瞬时完成** | `parallel-1`（isNonBlocking=true）| ✅ 成功（**假象**）|
| 非阻塞线程订阅 + **真实延迟** | `parallel-2`（isNonBlocking=true）| ❌ **抛异常** |

第三组实际异常（取证原文）：

```
AgentExecutionException :: agentscope execute failed:
  block()/blockFirst()/blockLast() are blocking, which is not supported in thread parallel-2
cause: java.lang.IllegalStateException（同上）
```

**结论**：触发需同时满足「订阅线程是 Reactor NonBlocking 线程」与「调用真的需要等待」。生产环境 LLM 调用必然耗时，故第一个条件成立即必然踩雷。第二组的"成功"是因为 mock 瞬时完成——**Reactor 只在需要实际等待时才做 NonBlocking 检查**，这是一个会骗人的假绿灯。

### F3. 图引擎**不**把节点动作调度到自己的线程池

`NodeExecutor` 字节码中确有 `Schedulers.fromExecutor(...)` 与 `Schedulers.parallel()` 兜底并 `Flux.subscribeOn(...)`，但实测节点动作**运行在调用方线程**（`main`）。那段调度属于 embedded-flux 处理路径，不是普通节点路径。（此条推翻了"引擎会调度到 parallel() 所以必然踩雷"的早期推断。）

### F4. 可达性

- **ddd4j-ai 仓内当前不可达**：全仓无 `spring-webflux` 依赖，无 Netty 事件循环线程。
- **对业务消费方可达**：任何在 WebFlux/Netty 或 Reactor 调度器线程上订阅 `flowService.stream(...)` / 调用同步 API 的消费方都会踩雷。

### F5. 测试盲区（缺陷长期隐形的原因）

`GraphFlowServiceTest:139` 的 AGENT 节点用例用 `mock(AgentService.class)`（瞬时返回、内部无 `.block()`），因此真实 adapter 的这条路径**从未被覆盖**。

### F6. `executeTask` 的内层阻塞是自死锁隐患

`AgentPlanOrchestrator:123` 写成 `Mono.fromCallable(() -> harnessAgent.call(...).block())`——把阻塞调用包进 `Mono.fromCallable`，再由 `Flux.merge` 组合。从非阻塞线程订阅时，内层 `.block()` 抛异常（同 F2 机制）。

## 3. 目标

- 为 `AgentService` 提供**非阻塞入口**，使 WebFlux/Reactor 消费方有正规用法。
- **消除 `GraphFlowService` AGENT 节点路径的阻塞隐患**——该修复对任何消费方生效，不依赖消费方是否遵守约定。
- 让同步 API 在非阻塞线程上**失败得可读**（带指引的异常），而非抛出费解的 `block() is blocking`。
- 消除 `executeTask` 的自死锁隐患。
- 保持既有 4 个同步签名的**行为与二进制兼容**，不破坏现有调用方。

### 非目标

- **不引入 `spring-webflux`**。本项所需只是 `Schedulers.boundedElastic()`（来自已在依赖中的 `reactor-core`）。给库扩展加 `spring-webflux` 会传递性拉入 Netty 与响应式 Web 栈，并可能与消费方的 Spring MVC 应用冲突——典型的不必要依赖膨胀。验证非阻塞行为只需在 `Schedulers.parallel()` 上订阅，无需 WebFlux。
- 不把 `execute` / `dispatchAll` / `mergeResults` 改成 reactive（破坏兼容）；只**新增** reactive 入口。
- 不改 `FlowService` 接口签名（`run` 仍同步返回 `Map`、`stream` 仍返回 `Flux`）。
- 不做 Micrometer 埋点（属第 5 项）。

## 4. 关键决策

### 决策 1：新增 default 方法承载 reactive 入口（而非改签名）

沿用第 1 项已验证的模式：在 SPI 上加 `default` 方法，默认实现把既有阻塞方法**卸载到 `boundedElastic`**。

```java
default Mono<AgentResult> executeAsync(AgentTask task) {
    return Mono.fromCallable(() -> execute(task))
            .subscribeOn(Schedulers.boundedElastic());
}
```

好处：任何既有第三方 `AgentService` 实现**无需改动**即获得可用的异步入口（`boundedElastic` 容忍阻塞）；`AgentScopeAgentAdapter` 再覆写为真正非阻塞实现（见决策 2）。

为何 `boundedElastic` 而非 `parallel`：`boundedElastic` 专为包装阻塞调用设计，其线程不在 Reactor NonBlocking 集合内，`.block()` 在其中合法。

### 决策 2：`AgentScopeAgentAdapter` 覆写为**零阻塞**实现

不满足于"把 `.block()` 搬到别的线程"，而是复用 Agentscope 的 reactive 能力：

```java
@Override
public Mono<AgentResult> executeAsync(AgentTask task) {
    Objects.requireNonNull(task, "task");
    return harnessAgent.call(new UserMessage(task.instruction()))
            .map(response -> toResult(response, task.conversationId()))
            .onErrorMap(RuntimeException.class, e -> new AgentExecutionException(
                    "agentscope execute failed: " + e.getMessage(), e));
}
```

为此把 `execute` 里构造 `AgentResult` 的逻辑抽成 `toResult(Msg, String conversationId)`，`execute` 与 `executeAsync` 共用——消除重复，且保证两条路径产出一致。

### 决策 3：`GraphFlowService.agentAction` 改走 reactive 路径

`AsyncNodeAction` 本就返回 `CompletableFuture`，把同步调用换成 `executeAsync(...).toFuture()` 即可**彻底移除该路径的阻塞**：

```java
return agentService.executeAsync(AgentTask.of(instruction))
        .map(result -> Map.<String, Object>of(spec.outputKey(), result.output()))
        .toFuture();
```

这是本项**最高价值**的一处改动：它让 AGENT 节点在任何订阅线程上都安全，且不要求消费方改变用法。

### 决策 4：同步方法加非阻塞线程守卫

新增 `io.ddd4j.ai.extension.agent.util.BlockingCallGuard`：

```java
public static void requireBlockingCapableThread(String asyncAlternative) {
    if (Schedulers.isInNonBlockingThread()) {
        throw new IllegalStateException(
            "当前线程 " + Thread.currentThread().getName() + " 是 Reactor 非阻塞线程，"
            + "不可调用阻塞 API（会抛 block() is blocking）。请改用 " + asyncAlternative
            + "，或在 boundedElastic 等可阻塞调度器上调用。");
    }
}
```

接入点：`AgentScopeAgentAdapter.execute`、`AgentPlanOrchestrator.dispatchAll` / `mergeResults`、`GraphFlowService.run`（其内部 `blockLast()` 同理）。

为什么值得做：F2 的原始异常信息（`block()/blockFirst()/blockLast() are blocking, which is not supported in thread parallel-2`）不告诉调用方**该改用什么**。守卫把"费解的框架报错"换成"可执行的指引"，成本极低。

### 决策 5：`dispatchAllAsync` 与 `dispatchAll` 共用一条实现

避免两份容易漂移的逻辑：

```java
public Mono<Map<String, String>> dispatchAllAsync(String planId) { ...真正实现，不 block... }

public Map<String, String> dispatchAll(String planId) {
    BlockingCallGuard.requireBlockingCapableThread("dispatchAllAsync(planId)");
    return dispatchAllAsync(planId).block();
}
```

同时把 `executeTask` 的内层 `.block()` 改为直接使用 reactive 链（`harnessAgent.call(...)` 的 `Mono` 直接 `map`），消除 F6 的自死锁隐患。

## 5. 接口方向

```java
// AgentService.java —— 新增 default 方法；既有 3 个方法不动
default Mono<AgentResult> executeAsync(AgentTask task) {
    return Mono.fromCallable(() -> execute(task))
            .subscribeOn(Schedulers.boundedElastic());
}

// AgentPlanOrchestrator.java —— 新增 reactive 入口
public Mono<Map<String, String>> dispatchAllAsync(String planId);

// GraphFlowService.agentAction —— 内部改走 reactive（签名不变）
// 返回 CompletableFuture<Map<String,Object>>，由 executeAsync(...).toFuture() 产出
```

## 6. 模块落点

| 文件 | 动作 | 说明 |
|------|------|------|
| `ddd4j-ai-extension-agent/.../service/AgentService.java` | 改 | 加 import + `default executeAsync` |
| `ddd4j-ai-extension-agent/.../agent/AgentScopeAgentAdapter.java` | 改 | 抽 `toResult(Msg, String)`；覆写 `executeAsync`；`execute` 加守卫 |
| `ddd4j-ai-extension-agent/.../util/BlockingCallGuard.java` | 新增 | 非阻塞线程守卫（`public`，供 agent / flow 两模块共用——flow 已 compile 依赖 agent 扩展）|
| `ddd4j-ai-extension-agent/.../dispatch/AgentPlanOrchestrator.java` | 改 | 加 `dispatchAllAsync`；`dispatchAll` 改为委托 + 守卫；`mergeResults` 加守卫；`executeTask` 去内层阻塞 |
| `ddd4j-ai-extension-flow/.../flow/service/impl/GraphFlowService.java` | 改 | `agentAction` 改走 `executeAsync(...).toFuture()`；`run` 加守卫 |
| 各模块对应测试 | 改 | 见第 7 节 |

## 7. 测试策略

全部离线，不依赖网络/真实 LLM。核心手段：**在 `Schedulers.parallel()` 上订阅**以复现 NonBlocking 线程条件（与 F2 探针同一手法）。

| 测试类 | 新增用例 | 断言要点 |
|--------|---------|---------|
| `AgentServiceContractTest` | `executeAsync_defaultOffloadsToBoundedElastic` | 匿名实现（仅 `execute`），在 `parallel()` 上订阅 `executeAsync` 得正确 `AgentResult` 且不抛 |
| `AgentScopeAgentAdapterTest` | `executeAsync_nonBlockingThread_succeeds` | mock `HarnessAgent` 返回带延迟的 `Mono`；在 `parallel()` 上订阅成功（对照：此时 `execute` 会抛）|
| `AgentScopeAgentAdapterTest` | `execute_onNonBlockingThread_throwsGuidedError` | 在 `parallel()` 上直接调 `execute` → 抛 `IllegalStateException` 且 message 含 `executeAsync` 指引 |
| `AgentPlanOrchestratorTest` | `dispatchAllAsync_nonBlockingThread_succeeds` | 在 `parallel()` 上订阅 `dispatchAllAsync` 得结果 Map |
| `AgentPlanOrchestratorTest` | `dispatchAll_onNonBlockingThread_throwsGuidedError` | 守卫生效，message 含 `dispatchAllAsync` |
| `GraphFlowServiceTest` | `agentNode_withBlockingAgentService_subscribedOnParallelThread_succeeds` | **本项的核心回归锚点**：AGENT 节点 + 内部会 `.block()` 的 AgentService，在 `parallel()` 上订阅整个 flow → 成功（修复前按 F2 必抛）|
| `BlockingCallGuardTest` | `requireBlockingCapableThread_allowsOnBoundedElastic` / `throwsOnParallel` | 守卫本体 |

### 验收标准

1. 上述用例全部通过；两个模块既有测试**零回归**。
2. **核心证据**：`agentNode_withBlockingAgentService_subscribedOnParallelThread_succeeds` 通过——它精确复现 F2 的失败条件并证明已修复。
3. `AgentService` / `FlowService` 既有签名未变；第 1 项的 `streamEvents` 相关测试保持通过。
4. 两线同步（1.0.x 受第 9 节残留项阻塞时，先只交付 2.0.x 并如实标注）。

### 构建与验证命令

```bash
export JAVA_HOME=/Users/wandl/Library/Java/JavaVirtualMachines/corretto-17.0.20.1/Contents/Home
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
# 2.0.x 用 Maven 4
/Users/wandl/tools/apache-maven-4.0.0-rc-6/bin/mvn \
  -pl ddd4j-ai-extensions/ddd4j-ai-extension-agent,ddd4j-ai-extensions/ddd4j-ai-extension-flow \
  -Denforcer.skip=true -DskipITs test
```

## 8. 风险与缓解

| 风险 | 等级 | 缓解 |
|------|------|------|
| 守卫误伤：某些"非阻塞线程"实际允许阻塞 | 低 | 仅在 `Schedulers.isInNonBlockingThread()` 为真时抛，该判断由 Reactor 提供，语义明确 |
| `executeAsync` 默认实现把阻塞搬到 boundedElastic，可能与调用方期望的线程语义不符 | 低 | 文档写明；真非阻塞路径由 `AgentScopeAgentAdapter` 覆写提供 |
| `GraphFlowService.run` 的守卫会改变既有同步调用行为 | 中 | 仅在非阻塞线程上抛（原本就必然失败）；阻塞线程行为不变。既有测试均在 main 线程，不受影响 |
| 1.0.x 无法构建，移植受阻 | 高 | 见第 9 节；先交付 2.0.x |

## 9. 前置残留项（与本项耦合）

**1.0.x 线在本机无法构建**，因此第 2 项的移植与第 1 项同样受阻。根因（第 1 项已查明并部分修复）：
- 已修：`~/.m2` 中 `ddd4j-parent` / `ddd4j-dependencies` 的 `2.0.x.20260730-SNAPSHOT` 缓存 POM 把父版本写成字面 `${revision}`（备份在 `/tmp`）。
- **未修（需授权）**：`feature/1.0.x` 分支自身的 POM 缺陷——`ddd4j-ai-samples/pom.xml:66` 与 `ddd4j-ai-sdk-deps/pom.xml:20/25/30` 依赖缺 version 且无依赖管理条目。修它们需要改 pom，与"本项不改 pom"无关但需单独授权。

## 10. 后续项

3. `AgentDispatchTaskRepository` 持久化实现
4. TTS / ASR 双 Router 收敛
5. Micrometer 可观测性专项
6. 工程卫生批次

---

## 11. 实施记录（2026-09-11）

### 已完成（feature/2.0.x，已推 GitHub + Codeup）

| Task | 提交 | 验证 |
|------|------|------|
| 1 `BlockingCallGuard` | `c71fd3d` | `BlockingCallGuardTest` 3/3 |
| 2 `AgentService.executeAsync` default | `2b3b642` | `AgentServiceContractTest` 7/7 |
| 3 `AgentScopeAgentAdapter` 零阻塞覆写 + 守卫 + `toResult` | `fe7ea4c` | `AgentScopeAgentAdapterTest` 13/13 |
| 4 `AgentPlanOrchestrator` reactive 化 | `82c4ae0` | `AgentPlanOrchestratorTest` 9/9 |
| 5 `GraphFlowService` AGENT 节点改 reactive | `28dde96` | `GraphFlowServiceTest` 7/7 |

回归：agent 模块 **50/50**、flow 模块 **15/15**，含上游全 reactor（core/memory/chat/agent/flow）`BUILD SUCCESS`。

### 核心证据（缺陷被复现然后修好）

Task 5 的锚点测试 `agentNode_withBlockingAgentService_subscribedOnNonBlockingThread_succeeds` 精确复现了 §2 F2 的失败条件（真实 adapter + 200ms 延迟 + `parallel()` 订阅）：

- **修复前**（但 Task 3 的守卫已生效）：抛出
  `IllegalStateException: 当前线程 parallel-1 是 Reactor 非阻塞线程，不可调用阻塞 API（Reactor 会抛 block() is blocking）。请改用 executeAsync(task)…`
  —— 注意此时已是**守卫的可读信息**而非 Reactor 原生报错，守卫的价值在此可见。
- **修复后**：测试通过（`agent_out=flow answer`）。

顺带验证：`GraphFlowService.run` 的守卫按设计只在非阻塞线程触发；既有 6 个在 main 线程运行的用例零回归。

### 实施中发现的三处非预期事实（值得记录）

1. **flow 模块编译时解析的是 `~/.m2` 里已安装的 agent 扩展 jar，而非 reactor 源码**。首次跑 Task 5 的 Step 2 时，报错是 Reactor 原生 `block()` 信息而非 Task 3 新加的守卫——说明 flow 看到的是**旧版 adapter**。加 `-am`（`mvn -pl <flow> -am`）后才从源码构建。**凡改动 agent 扩展再跑 flow 测试，必须带 `-am`，否则测试对着旧 jar 跑，结论无效。**
2. **Mockito mock 不执行接口 default 方法**。既有用例 `agentNode_executesAgentService` 用 `mock(AgentService.class)` 且只 stub 了 `execute`；改用 `executeAsync` 后该方法返回 `null`（而非命中接口 default），导致 `NullPointerException`。已在用例中一并 stub `executeAsync`。**这是消费方/测试的通用注意点**：mock `AgentService` 时必须显式 stub `executeAsync`。生产实现（真实对象）不受影响，Java 会正常调用 default 方法。
3. **`thenReturn` 需类型见证**：`Mono.just(new AssistantMessage(...))` 推断为 `Mono<AssistantMessage>` 而形参是 `Mono<Msg>` → 编译失败，须写 `Mono.<Msg>just(...)`（`delayElement` 保留类型参数）。与第 1 项同类问题。

### 计划的两处系统性偏差（已记入计划 Global Constraints）

- **不用 `StepVerifier`**：实测 agent/flow 模块 classpath **无 `reactor-test`**（只有 `reactor-core`）。为避免为测试新增依赖，全部改用 `.block()` / `.blockLast()` + AssertJ。
- **不新增任何依赖**（含 `spring-webflux`）：本项仅用 `reactor-core` 的 `Schedulers`。

### Task 6 未完成：1.0.x 移植仍受阻

与第 1 项同一阻塞：1.0.x 分支的 `ddd4j-ai-samples/pom.xml:66` 与 `ddd4j-ai-sdk-deps/pom.xml:20/25/30` 有依赖缺 version 的既有缺陷，且本机 `~/.m2` 无 1.0.x 产物（只有失败下载残留），需从零构建整条 reactor。**修这些 POM 需单独授权**（本项改动不涉及 pom，该授权是范围外的）。因此本次仅交付 2.0.x，1.0.x 待授权后按第 1 项同样方式移植。

### 环境提示

- 磁盘已清理至 84%（曾因 97% 导致 `MysqlStateStoreWiringTest` 容器 `exit 1`，清理后 2/2 通过）。
- 构建命令注意：surefire 的跳过标志是 `-Dsurefire.failIfNoSpecifiedTests=false`（不是 `-DfailIfNoSpecifiedTests`），写错会让上游无匹配测试的模块直接失败。
- `ddd4j-ai-extension-chat` 模块测试耗时约 8 分钟（本次全 reactor 构建中观察），单独调试时建议用 `-pl` 限定范围。
