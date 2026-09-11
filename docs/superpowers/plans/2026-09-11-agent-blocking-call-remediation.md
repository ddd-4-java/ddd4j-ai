# Agent 阻塞调用治理实施计划（reactive 入口 + Flow 路径修复）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 消除 `AgentService.execute` / `AgentPlanOrchestrator` 的 `.block()` 在 Reactor 非阻塞线程上的必然失败，并为消费方提供正规的非阻塞入口。

**Architecture:** 双管齐下——(1) **消除**仓内唯一可达路径的阻塞：`GraphFlowService.agentAction` 改走 reactive（`executeAsync(...).toFuture()`），`AgentPlanOrchestrator` 内部全部改为 reactive 链；(2) **提供**非阻塞入口：`AgentService.executeAsync` 以 `default` 方法给出（既有实现零改动即获得可用异步入口），`AgentScopeAgentAdapter` 覆写为真正零阻塞实现。同步 API 保留，加 `BlockingCallGuard` 把费解的框架报错换成带指引的异常。

**Tech Stack:** Java 17、Reactor（`Mono` / `Schedulers.boundedElastic()` / `Schedulers.parallel()`）、AgentScope Java 2.0.2、JUnit 5 + AssertJ + Mockito、Maven（2.0.x 用 4.0.0-rc-6）。

**Spec:** `docs/superpowers/specs/2026-09-11-agent-blocking-call-remediation-design.md`

## Global Constraints

- **Java 17**；两条线均 `<java.version>17</java.version>`。
- **构建**：`feature/2.0.x` 用 `/Users/wandl/tools/apache-maven-4.0.0-rc-6/bin/mvn`（`modelVersion 4.1.0`，Maven 3 报 `Malformed POM`）；`feature/1.0.x` 用 Maven 3。
- **JAVA_HOME 必须为 JDK 17**：`/Users/wandl/Library/Java/JavaVirtualMachines/corretto-17.0.20.1/Contents/Home`。
- **不引入 `spring-webflux`**（spec §3 非目标）：本项只需 `reactor-core` 的 `Schedulers`，已在依赖中。验证非阻塞行为用 `Schedulers.parallel()` 订阅即可。
- **测试断言统一用 `.block()` / `.blockLast()` + AssertJ，不用 `StepVerifier`**：实测依赖树显示 agent 与 flow 模块只有 `reactor-core`、**无 `reactor-test`**。为避免为测试新增依赖，本项所有用例改用等价的阻塞断言（`assertThatThrownBy` 等）。计划正文示例若出现 `StepVerifier`，以本约束为准改写。
- **`thenReturn` 必须加类型见证**：`Mono.just(new AssistantMessage(...))` 会推断为 `Mono<AssistantMessage>`，而 Mockito 的 `thenReturn` 形参是 `Mono<Msg>` → **编译失败**。统一写成 `Mono.<Msg>just(...)`（`delayElement` 会保留类型参数）。Task 3/4/5 的示例均需如此。
- **既有 4 个同步签名行为不变**：`AgentService.execute` / `stream`、`AgentPlanOrchestrator.dispatchAll` / `mergeResults`、`FlowService.run` / `stream`。只**新增** reactive 方法 + 加守卫。
- **禁止 `git add -A` / `git add .`**：只显式 stage 本任务列出的路径。
- **磁盘**：本机曾因磁盘满导致容器测试 `ContainerLaunchException`，现已清理（84%）。若再遇容器类测试失败，先查磁盘而非怀疑代码。
- 所有测试**离线可跑**，不依赖网络 / 真实 LLM / API key。

---

## File Structure

| 文件 | 职责 | 动作 |
|------|------|------|
| `ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/main/java/io/ddd4j/ai/extension/agent/util/BlockingCallGuard.java` | 非阻塞线程守卫（`public`，agent/flow 共用）| 新增 |
| `.../agent/src/main/java/io/ddd4j/ai/extension/agent/service/AgentService.java` | 加 `default executeAsync` | 改 |
| `.../agent/src/main/java/io/ddd4j/ai/extension/agent/agent/AgentScopeAgentAdapter.java` | 抽 `toResult`；覆写 `executeAsync`；`execute` 加守卫 | 改 |
| `.../agent/src/main/java/io/ddd4j/ai/extension/agent/dispatch/AgentPlanOrchestrator.java` | 加 `dispatchAllAsync` / `mergeResultsAsync`；`executeTask` 去阻塞；同步方法加守卫 | 改 |
| `.../flow/src/main/java/io/ddd4j/ai/extension/flow/service/impl/GraphFlowService.java` | `agentAction` 改 reactive；`run` 加守卫 | 改 |
| 5 个对应测试类 | 见各 Task | 改 |

**关键设计说明（供实施者理解）**：
- `boundedElastic` 是 Reactor 专为包装阻塞调用设计的调度器，其线程**不在** NonBlocking 集合内，因此 `.block()` 在其中合法——这是 `executeAsync` 默认实现的立足点。
- 触发本缺陷需要「订阅线程是 NonBlocking 线程」AND「调用真的需要等待」。**瞬时完成的 mock 会掩盖失败**（Reactor 只在需要实际等待时才做 NonBlocking 检查）——因此所有验证非阻塞行为的测试**必须给被测调用注入延迟**，否则测试会给出骗人的绿灯。
- `GraphFlowService` 所在模块已 compile 依赖 agent 扩展，可直接引用 `BlockingCallGuard`。

---

## Task 1: BlockingCallGuard

**Files:**
- Create: `ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/main/java/io/ddd4j/ai/extension/agent/util/BlockingCallGuard.java`
- Test: `ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/test/java/io/ddd4j/ai/extension/agent/util/BlockingCallGuardTest.java`

**Interfaces:**
- Produces: `BlockingCallGuard.requireBlockingCapableThread(String asyncAlternative)` —— 在 Reactor NonBlocking 线程上抛 `IllegalStateException`，message 含传入的替代方案名；其余情况返回

- [ ] **Step 1: 写失败测试**

```java
package io.ddd4j.ai.extension.agent.util;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class BlockingCallGuardTest {

    @Test
    void allowsOnBoundedElasticThread() {
        // boundedElastic 是 Reactor 为包装阻塞调用设计的调度器，守卫应放行
        StepVerifier.create(Mono.fromCallable(() -> {
                    BlockingCallGuard.requireBlockingCapableThread("executeAsync(task)");
                    return "ok";
                }).subscribeOn(Schedulers.boundedElastic()))
                .expectNext("ok")
                .verifyComplete();
    }

    @Test
    void allowsOnPlainCallerThread() {
        // 普通调用方线程（非 NonBlocking）应放行
        BlockingCallGuard.requireBlockingCapableThread("executeAsync(task)");
    }

    @Test
    void throwsOnParallelThreadWithActionableMessage() {
        StepVerifier.create(Mono.fromCallable(() -> {
                    BlockingCallGuard.requireBlockingCapableThread("executeAsync(task)");
                    return "unreachable";
                })
                // 加延迟确保 Reactor 真的在该线程上执行（瞬时完成会掩盖 NonBlocking 检查）
                .delayElement(Duration.ofMillis(50))
                .subscribeOn(Schedulers.parallel()))
                .expectErrorSatisfies(err -> {
                    assertThat(err).isInstanceOf(IllegalStateException.class);
                    assertThat(err.getMessage())
                            .contains("非阻塞线程")
                            .contains("executeAsync(task)");
                })
                .verify();
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
export JAVA_HOME=/Users/wandl/Library/Java/JavaVirtualMachines/corretto-17.0.20.1/Contents/Home
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
/Users/wandl/tools/apache-maven-4.0.0-rc-6/bin/mvn \
  -pl ddd4j-ai-extensions/ddd4j-ai-extension-agent \
  -Dtest=BlockingCallGuardTest -DfailIfNoSpecifiedTests=false \
  -Denforcer.skip=true -DskipITs test
```

Expected: 编译失败，`找不到符号: 类 BlockingCallGuard`。

- [ ] **Step 3: 实现**

```java
package io.ddd4j.ai.extension.agent.util;

import reactor.core.scheduler.Schedulers;

/**
 * 阻塞调用守卫：把 Reactor 在非阻塞线程上拒绝 {@code .block()} 时抛出的费解报错，
 * 换成带可执行指引的异常。
 *
 * <p>Reactor 只在调用**真的需要等待**时才做 NonBlocking 检查，故本守卫的失败
 * 无法被"瞬时完成的 mock"掩盖——也正因如此，触发它必须让调用真正耗时。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public final class BlockingCallGuard {

    private BlockingCallGuard() {
    }

    /**
     * 校验当前线程可以执行阻塞调用。
     *
     * @param asyncAlternative 出问题时提示调用方改用的替代方案（如 {@code "executeAsync(task)"}）
     * @throws IllegalStateException 当前线程是 Reactor NonBlocking 线程
     */
    public static void requireBlockingCapableThread(String asyncAlternative) {
        if (Schedulers.isInNonBlockingThread()) {
            throw new IllegalStateException(
                    "当前线程 " + Thread.currentThread().getName() + " 是 Reactor 非阻塞线程，"
                            + "不可调用阻塞 API（Reactor 会抛 block() is blocking）。"
                            + "请改用 " + asyncAlternative
                            + "，或在 Schedulers.boundedElastic() 等可阻塞调度器上调用。");
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

同 Step 2 命令。Expected: `Tests run: 3, Failures: 0, Errors: 0`。

- [ ] **Step 5: 提交**

```bash
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
git add \
  ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/main/java/io/ddd4j/ai/extension/agent/util/BlockingCallGuard.java \
  ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/test/java/io/ddd4j/ai/extension/agent/util/BlockingCallGuardTest.java
git commit -m "feat(agent): add BlockingCallGuard for actionable non-blocking-thread errors

Turns Reactor's opaque 'block()/blockFirst()/blockLast() are blocking, which is
not supported in thread <name>' into an error that names the async alternative
the caller should use instead."
```

---

## Task 2: AgentService.executeAsync

**Files:**
- Modify: `ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/main/java/io/ddd4j/ai/extension/agent/service/AgentService.java`
- Test: `ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/test/java/io/ddd4j/ai/extension/agent/service/AgentServiceContractTest.java`

**Interfaces:**
- Consumes: 既有 `AgentService.execute(AgentTask)`
- Produces: `AgentService.executeAsync(AgentTask) → Mono<AgentResult>`，默认把 `execute` 卸载到 `boundedElastic`

- [ ] **Step 1: 写失败测试**

在 `AgentServiceContractTest` 追加（该类已有 `FakeAgentService`，它只实现 `execute` / `stream`，正好命中新 default）：

```java
    @Test
    void executeAsync_runsExecuteOnBoundedElasticThread() {
        java.util.concurrent.atomic.AtomicReference<String> threadName = new java.util.concurrent.atomic.AtomicReference<>();
        AgentService service = new AgentService() {
            @Override
            public AgentResult execute(AgentTask task) {
                threadName.set(Thread.currentThread().getName());
                return new AgentResult("done", List.of(), task.conversationId());
            }

            @Override
            public Flux<AgentStep> stream(AgentTask task) {
                return Flux.empty();
            }
        };

        AgentResult result = service.executeAsync(AgentTask.of("task")).block();

        assertThat(result).isNotNull();
        assertThat(result.output()).isEqualTo("done");
        // 关键：默认实现必须把阻塞的 execute 卸载到 boundedElastic，而非调用方线程
        assertThat(threadName.get()).startsWith("boundedElastic");
    }
```

- [ ] **Step 2: 运行测试确认失败**

```bash
export JAVA_HOME=/Users/wandl/Library/Java/JavaVirtualMachines/corretto-17.0.20.1/Contents/Home
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
/Users/wandl/tools/apache-maven-4.0.0-rc-6/bin/mvn \
  -pl ddd4j-ai-extensions/ddd4j-ai-extension-agent \
  -Dtest=AgentServiceContractTest -DfailIfNoSpecifiedTests=false \
  -Denforcer.skip=true -DskipITs test
```

Expected: 编译失败，`找不到符号: 方法 executeAsync(AgentTask)`。

- [ ] **Step 3: 实现**

在 `AgentService.java` 的 import 区加（`io.agentscope.core.event.AgentEvent` 之后、`reactor.core.publisher.Flux` 之前保持字典序）：

```java
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
```

在接口体末尾（`default Flux<AgentEvent> streamEvents(...)` 之后）追加：

```java
    /**
     * 非阻塞执行：为 WebFlux / Reactor 消费方提供的正规入口。
     *
     * <p>默认实现把阻塞的 {@link #execute(AgentTask)} 卸载到
     * {@link Schedulers#boundedElastic()}——该调度器专为包装阻塞调用设计，
     * 其线程不在 Reactor 的 NonBlocking 集合内，因此 {@code execute} 可安全阻塞。
     * 既有第三方实现无需改动即获得可用的异步入口。
     *
     * <p>真正的零阻塞实现由 {@code AgentScopeAgentAdapter} 覆写提供。
     *
     * @param task 智能体任务
     * @return 结果（非阻塞）
     */
    default Mono<AgentResult> executeAsync(AgentTask task) {
        return Mono.fromCallable(() -> execute(task))
                .subscribeOn(Schedulers.boundedElastic());
    }
```

- [ ] **Step 4: 运行测试确认通过**

同 Step 2 命令。Expected: `Tests run: 7, Failures: 0, Errors: 0`（原 6 例 + 新 1 例）。

- [ ] **Step 5: 提交**

```bash
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
git add \
  ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/main/java/io/ddd4j/ai/extension/agent/service/AgentService.java \
  ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/test/java/io/ddd4j/ai/extension/agent/service/AgentServiceContractTest.java
git commit -m "feat(agent): add default AgentService.executeAsync port method

Gives WebFlux/Reactor consumers a sanctioned non-blocking entry point. The
default offloads the blocking execute() onto Schedulers.boundedElastic(), which
tolerates blocking, so existing third-party implementations gain a working async
path without any change."
```

---

## Task 3: AgentScopeAgentAdapter — toResult 抽取 + executeAsync 覆写 + 守卫

**Files:**
- Modify: `ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/main/java/io/ddd4j/ai/extension/agent/agent/AgentScopeAgentAdapter.java`
- Test: `ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/test/java/io/ddd4j/ai/extension/agent/agent/AgentScopeAgentAdapterTest.java`

**Interfaces:**
- Consumes: Task 1 的 `BlockingCallGuard`；Task 2 的 `executeAsync` 契约
- Produces: `AgentScopeAgentAdapter.executeAsync` 覆写（零阻塞）；`execute` 加守卫；`toResult(Msg, String)` 私有助手（两条路径共用）

- [ ] **Step 1: 写失败测试**

在 `AgentScopeAgentAdapterTest` 追加（该文件已 import `Mono`、`mock`、`when`、`any`、`assertThat`、`assertThatThrownBy`；需补 `Schedulers` 与 `Duration`）：

```java
    @Test
    void executeAsync_nonBlockingThread_succeeds() {
        HarnessAgent harness = mock(HarnessAgent.class);
        // 加延迟：瞬时完成会掩盖 NonBlocking 检查，必须让调用真的耗时
        when(harness.call(any(Msg.class)))
                .thenReturn(Mono.just(new AssistantMessage("async answer"))
                        .delayElement(java.time.Duration.ofMillis(200)));

        AgentScopeAgentAdapter adapter = new AgentScopeAgentAdapter(harness);

        String output = adapter.executeAsync(io.ddd4j.ai.extension.agent.service.AgentTask.of("hi"))
                .subscribeOn(reactor.core.scheduler.Schedulers.parallel())
                .map(r -> r.output())
                .block();

        assertThat(output).isEqualTo("async answer");
    }

    @Test
    void execute_onNonBlockingThread_throwsGuidedError() {
        HarnessAgent harness = mock(HarnessAgent.class);
        when(harness.call(any(Msg.class)))
                .thenReturn(Mono.just(new AssistantMessage("x")).delayElement(java.time.Duration.ofMillis(200)));

        AgentScopeAgentAdapter adapter = new AgentScopeAgentAdapter(harness);

        reactor.test.StepVerifier
                .create(Mono.fromCallable(() -> adapter.execute(
                                io.ddd4j.ai.extension.agent.service.AgentTask.of("hi")))
                        .subscribeOn(reactor.core.scheduler.Schedulers.parallel()))
                .expectErrorSatisfies(err -> {
                    assertThat(err).isInstanceOf(IllegalStateException.class);
                    assertThat(err.getMessage()).contains("executeAsync");
                })
                .verify();
    }
```

- [ ] **Step 2: 运行测试确认失败**

```bash
export JAVA_HOME=/Users/wandl/Library/Java/JavaVirtualMachines/corretto-17.0.20.1/Contents/Home
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
/Users/wandl/tools/apache-maven-4.0.0-rc-6/bin/mvn \
  -pl ddd4j-ai-extensions/ddd4j-ai-extension-agent \
  -Dtest=AgentScopeAgentAdapterTest -DfailIfNoSpecifiedTests=false \
  -Denforcer.skip=true -DskipITs test
```

Expected: 至少 `execute_onNonBlockingThread_throwsGuidedError` FAIL。原因：`executeAsync` 由 default 提供（卸载到 boundedElastic），第一例可能已通过；但 `execute` 尚无守卫，在 `parallel()` 上抛的是 Reactor 原生 `IllegalStateException`（message 为 `block()/blockFirst()/blockLast() are blocking...`），**不含** `executeAsync` 指引 → 断言失败。

- [ ] **Step 3: 实现**

(a) import 区加：

```java
import io.ddd4j.ai.extension.agent.util.BlockingCallGuard;
```

(b) 把 `execute` 重写为使用新助手 + 守卫：

```java
    @Override
    public AgentResult execute(AgentTask task) {
        Objects.requireNonNull(task, "task");
        BlockingCallGuard.requireBlockingCapableThread("executeAsync(task)");
        try {
            Msg response = harnessAgent.call(new UserMessage(task.instruction())).block();
            return toResult(response, task.conversationId());
        } catch (RuntimeException e) {
            log.warn("agentscope execute failed: {}", e.getMessage());
            throw new AgentExecutionException("agentscope execute failed: " + e.getMessage(), e);
        }
    }

    /**
     * 零阻塞执行：直接复用 Agentscope 的 reactive 链，不经过任何 {@code .block()}。
     */
    @Override
    public Mono<AgentResult> executeAsync(AgentTask task) {
        Objects.requireNonNull(task, "task");
        return harnessAgent.call(new UserMessage(task.instruction()))
                .map(response -> toResult(response, task.conversationId()))
                .onErrorMap(RuntimeException.class, e -> {
                    log.warn("agentscope executeAsync failed: {}", e.getMessage());
                    return new AgentExecutionException("agentscope execute failed: " + e.getMessage(), e);
                });
    }

    /** 把 Agentscope 响应规整为 {@link AgentResult}（同步/异步两条路径共用，保证产出一致）。 */
    private static AgentResult toResult(Msg response, String conversationId) {
        List<AgentStep> steps = new ArrayList<>();
        if (response instanceof AssistantMessage assistant) {
            String text = extractText(assistant);
            steps.add(new AgentStep("result", text, 0));
            return new AgentResult(text, steps, conversationId);
        }
        String fallback = response == null ? "" : response.getClass().getSimpleName();
        steps.add(new AgentStep("result", fallback, 0));
        return new AgentResult(fallback, steps, conversationId);
    }
```

- [ ] **Step 4: 运行测试确认通过**

同 Step 2 命令。Expected: `Tests run: 13, Failures: 0, Errors: 0`（原 11 例 + 新 2 例）。

- [ ] **Step 5: 提交**

```bash
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
git add \
  ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/main/java/io/ddd4j/ai/extension/agent/agent/AgentScopeAgentAdapter.java \
  ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/test/java/io/ddd4j/ai/extension/agent/agent/AgentScopeAgentAdapterTest.java
git commit -m "feat(agent): override executeAsync with a truly non-blocking path

Reuses Agentscope's reactive call() chain directly instead of offloading a
blocking call, and extracts toResult(Msg, String) so execute() and executeAsync()
produce identical results. execute() now guards against non-blocking threads with
an actionable message."
```

---

## Task 4: AgentPlanOrchestrator — reactive 化 + 守卫

**Files:**
- Modify: `ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/main/java/io/ddd4j/ai/extension/agent/dispatch/AgentPlanOrchestrator.java`
- Test: `ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/test/java/io/ddd4j/ai/extension/agent/dispatch/AgentPlanOrchestratorTest.java`

**Interfaces:**
- Consumes: Task 1 的 `BlockingCallGuard`
- Produces: `dispatchAllAsync(String) → Mono<Map<String,String>>`；`mergeResultsAsync(String) → Mono<String>`；`dispatchAll` / `mergeResults` 改为守卫 + 委托；`executeTask` 去掉内层 `.block()`

- [ ] **Step 1: 写失败测试**

在 `AgentPlanOrchestratorTest` 追加。需要补 import：`reactor.core.scheduler.Schedulers`、`reactor.core.publisher.Mono`、`reactor.test.StepVerifier`、`java.time.Duration`、`io.ddd4j.ai.extension.agent.dispatch.InMemoryAgentDispatchTaskRepository`（后两者若已存在则不重复）。

```java
    @Test
    void dispatchAllAsync_nonBlockingThread_succeeds() {
        HarnessAgent harness = mock(HarnessAgent.class);
        when(harness.call(any(Msg.class)))
                .thenReturn(Mono.just(new AssistantMessage("done")).delayElement(Duration.ofMillis(150)));
        AgentPlanOrchestrator orchestrator =
                new AgentPlanOrchestrator(harness, new InMemoryAgentDispatchTaskRepository());

        String planId = orchestrator.submitPlan("goal", List.of("t1", "t2"));

        Map<String, String> results = orchestrator.dispatchAllAsync(planId)
                .subscribeOn(Schedulers.parallel())
                .block();

        assertThat(results).hasSize(2);
        assertThat(results.values()).allMatch("done"::equals);
    }

    @Test
    void dispatchAll_onNonBlockingThread_throwsGuidedError() {
        HarnessAgent harness = mock(HarnessAgent.class);
        when(harness.call(any(Msg.class)))
                .thenReturn(Mono.just(new AssistantMessage("x")).delayElement(Duration.ofMillis(150)));
        AgentPlanOrchestrator orchestrator =
                new AgentPlanOrchestrator(harness, new InMemoryAgentDispatchTaskRepository());
        String planId = orchestrator.submitPlan("goal", List.of("t1"));

        StepVerifier.create(Mono.fromCallable(() -> orchestrator.dispatchAll(planId))
                        .subscribeOn(Schedulers.parallel()))
                .expectErrorSatisfies(err -> {
                    assertThat(err).isInstanceOf(IllegalStateException.class);
                    assertThat(err.getMessage()).contains("dispatchAllAsync");
                })
                .verify();
    }
```

- [ ] **Step 2: 运行测试确认失败**

```bash
export JAVA_HOME=/Users/wandl/Library/Java/JavaVirtualMachines/corretto-17.0.20.1/Contents/Home
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
/Users/wandl/tools/apache-maven-4.0.0-rc-6/bin/mvn \
  -pl ddd4j-ai-extensions/ddd4j-ai-extension-agent \
  -Dtest=AgentPlanOrchestratorTest -DfailIfNoSpecifiedTests=false \
  -Denforcer.skip=true -DskipITs test
```

Expected: 编译失败，`找不到符号: 方法 dispatchAllAsync(String)`。

- [ ] **Step 3: 实现**

(a) import 区加：`import io.ddd4j.ai.extension.agent.util.BlockingCallGuard;` 与 `import reactor.core.publisher.Mono;`（`Mono` 若已存在则不重复）。

(b) 把 `dispatchAll` 拆成 reactive 核心 + 同步壳：

```java
    /**
     * 非阻塞派发：为 WebFlux / Reactor 消费方提供。
     */
    public Mono<Map<String, String>> dispatchAllAsync(String planId) {
        Objects.requireNonNull(planId, "planId");
        return Mono.fromCallable(() -> {
                    List<AgentDispatchTask> pending = repository.findByPlanId(planId).stream()
                            .filter(t -> AgentDispatchTask.PENDING.equals(t.status()))
                            .toList();
                    if (pending.isEmpty()) {
                        throw new AgentExecutionException("no pending tasks for plan: " + planId);
                    }
                    pending.forEach(t -> repository.update(t.withStatus(AgentDispatchTask.RUNNING, null)));
                    return pending;
                })
                .flatMap(tasks -> Flux.merge(tasks.stream().map(this::executeTask).toList()).collectList())
                .map(finished -> {
                    Map<String, String> results = new java.util.LinkedHashMap<>();
                    for (AgentDispatchTask task : finished) {
                        repository.update(task);
                        results.put(task.id(), task.result());
                    }
                    log.info("plan dispatched: planId={}, done={}", planId, results.size());
                    return results;
                });
    }

    /**
     * 同步派发（保留既有签名）：在非阻塞线程上会抛出带指引的异常。
     */
    public Map<String, String> dispatchAll(String planId) {
        BlockingCallGuard.requireBlockingCapableThread("dispatchAllAsync(planId)");
        return dispatchAllAsync(planId).block();
    }
```

(c) `mergeResults` 同样处理：

```java
    public Mono<String> mergeResultsAsync(String planId) {
        Objects.requireNonNull(planId, "planId");
        return Mono.fromCallable(() -> {
                    List<AgentDispatchTask> tasks = repository.findByPlanId(planId);
                    if (tasks.isEmpty()) {
                        throw new AgentExecutionException("no tasks for plan: " + planId);
                    }
                    for (AgentDispatchTask task : tasks) {
                        if (!AgentDispatchTask.DONE.equals(task.status())) {
                            throw new AgentExecutionException("plan not fully done: " + planId
                                    + ", task " + task.id() + " is " + task.status());
                        }
                    }
                    StringBuilder prompt = new StringBuilder("合并以下子任务结果为一个最终答案：\n");
                    for (AgentDispatchTask task : tasks) {
                        prompt.append("- ").append(task.instruction().replace('\n', ' '))
                                .append("\n  结果：").append(task.result()).append('\n');
                    }
                    return prompt.toString();
                })
                .flatMap(prompt -> harnessAgent.call(new UserMessage(prompt)))
                .map(merged -> merged == null ? "" : merged.getTextContent());
    }

    public String mergeResults(String planId) {
        BlockingCallGuard.requireBlockingCapableThread("mergeResultsAsync(planId)");
        return mergeResultsAsync(planId).block();
    }
```

(d) `executeTask` 去掉内层阻塞：

```java
    private Mono<AgentDispatchTask> executeTask(AgentDispatchTask task) {
        return harnessAgent.call(new UserMessage(task.instruction()))
                .map(response -> task.withStatus(AgentDispatchTask.DONE,
                        response == null ? "" : response.getTextContent()))
                .onErrorResume(e -> {
                    log.warn("task dispatch failed: id={}, error={}", task.id(), e.getMessage());
                    return Mono.just(task.withStatus(AgentDispatchTask.FAILED,
                            String.valueOf(e.getMessage())));
                });
    }
```

- [ ] **Step 4: 运行测试确认通过**

同 Step 2 命令。Expected: 全绿（原 7 例 + 新 2 例 = 9 例）。

- [ ] **Step 5: 提交**

```bash
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
git add \
  ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/main/java/io/ddd4j/ai/extension/agent/dispatch/AgentPlanOrchestrator.java \
  ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/test/java/io/ddd4j/ai/extension/agent/dispatch/AgentPlanOrchestratorTest.java
git commit -m "refactor(agent): make AgentPlanOrchestrator reactive-first

dispatchAll/mergeResults become thin guarded shells over new dispatchAllAsync/
mergeResultsAsync implementations, and executeTask drops its nested .block()
(which was a self-deadlock hazard). Sync signatures are unchanged; on a
non-blocking thread they now fail with a message naming the async alternative."
```

---

## Task 5: GraphFlowService — AGENT 节点改 reactive（核心修复）

**Files:**
- Modify: `ddd4j-ai-extensions/ddd4j-ai-extension-flow/src/main/java/io/ddd4j/ai/extension/flow/service/impl/GraphFlowService.java`
- Test: `ddd4j-ai-extensions/ddd4j-ai-extension-flow/src/test/java/io/ddd4j/ai/extension/flow/service/impl/GraphFlowServiceTest.java`

**Interfaces:**
- Consumes: Task 2 的 `executeAsync`；Task 1 的 `BlockingCallGuard`
- Produces: `agentAction` 返回由 `executeAsync(...).toFuture()` 产出的 `CompletableFuture`（签名不变，行为不再阻塞）；`run` 加守卫

- [ ] **Step 1: 写失败测试（本项核心回归锚点）**

在 `GraphFlowServiceTest` 追加。这个用例精确复现 spec §2 F2 的失败条件：**内部会阻塞的 AgentService + 非阻塞线程订阅**。

```java
    @Test
    void agentNode_withBlockingAgentService_subscribedOnNonBlockingThread_succeeds() throws Exception {
        // 模拟"内部会真正阻塞"的智能体（如真实 LLM 网络耗时）
        io.ddd4j.ai.extension.agent.service.AgentService blockingAgent =
                new io.ddd4j.ai.extension.agent.service.AgentService() {
                    @Override
                    public io.ddd4j.ai.extension.agent.service.AgentResult execute(
                            io.ddd4j.ai.extension.agent.service.AgentTask task) {
                        // 内层真实阻塞：修复前，这条链在 parallel 线程上会抛 block() is blocking
                        return reactor.core.publisher.Mono
                                .just(new io.agentscope.core.message.AssistantMessage("flow answer"))
                                .delayElement(java.time.Duration.ofMillis(200))
                                .block();
                    }

                    @Override
                    public reactor.core.publisher.Flux<io.ddd4j.ai.extension.agent.service.AgentStep> stream(
                            io.ddd4j.ai.extension.agent.service.AgentTask task) {
                        return reactor.core.publisher.Flux.empty();
                    }
                };

        GraphFlowService service = new GraphFlowService(chatService, List.of(), blockingAgent);
        FlowDefinition definition = FlowDefinition.builder()
                .name("agent-flow-nb")
                .node(FlowNodeSpec.builder().id("a").type(FlowNodeType.AGENT)
                        .prompt("研究 {topic}").outputKey("agent_out").build())
                .edge(new FlowEdge("START", "a"))
                .edge(new FlowEdge("a", "END"))
                .build();

        // 在 Reactor 非阻塞线程上订阅整个 flow —— 修复前此处必抛
        var last = service.stream(service.compile(definition), Map.of("topic", "AI"))
                .subscribeOn(reactor.core.scheduler.Schedulers.parallel())
                .blockLast();

        assertThat(last).isNotNull();
        assertThat(last.state().data()).containsEntry("agent_out", "flow answer");
    }
```

- [ ] **Step 2: 运行测试确认失败**

```bash
export JAVA_HOME=/Users/wandl/Library/Java/JavaVirtualMachines/corretto-17.0.20.1/Contents/Home
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
/Users/wandl/tools/apache-maven-4.0.0-rc-6/bin/mvn \
  -pl ddd4j-ai-extensions/ddd4j-ai-extension-flow \
  -Dtest=GraphFlowServiceTest -DfailIfNoSpecifiedTests=false \
  -Denforcer.skip=true -DskipITs test
```

Expected: 新用例 FAIL，错误链含 `block()/blockFirst()/blockLast() are blocking, which is not supported in thread parallel-N`（这正是要修掉的缺陷）。

- [ ] **Step 3: 实现**

(a) import 区加：`import io.ddd4j.ai.extension.agent.util.BlockingCallGuard;`

(b) 替换 `agentAction`：

```java
    /** AGENT 节点：prompt 作指令调 AgentService，走 reactive 路径以免在非阻塞线程上 .block()。 */
    private AsyncNodeAction agentAction(FlowNodeSpec spec) {
        return state -> {
            if (agentService == null) {
                return CompletableFuture.failedFuture(
                        new IllegalStateException("AgentService 未装配（AGENT 节点不可用）: " + spec.id()));
            }
            String instruction = buildPrompt(spec, state);
            return agentService.executeAsync(io.ddd4j.ai.extension.agent.service.AgentTask.of(instruction))
                    .map(result -> Map.<String, Object>of(spec.outputKey(), result.output()))
                    .toFuture();
        };
    }
```

(c) `run` 加守卫：

```java
    @Override
    public Map<String, Object> run(CompiledGraph graph, Map<String, Object> input) {
        BlockingCallGuard.requireBlockingCapableThread("stream(graph, input)");
        NodeOutput last = graph.stream(input).blockLast();
        if (last == null) {
            return Map.of();
        }
        return last.state().data();
    }
```

- [ ] **Step 4: 运行测试确认通过**

同 Step 2 命令。Expected: 全绿（原 6 例 + 新 1 例 = 7 例）。

- [ ] **Step 5: 提交**

```bash
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
git add \
  ddd4j-ai-extensions/ddd4j-ai-extension-flow/src/main/java/io/ddd4j/ai/extension/flow/service/impl/GraphFlowService.java \
  ddd4j-ai-extensions/ddd4j-ai-extension-flow/src/test/java/io/ddd4j/ai/extension/flow/service/impl/GraphFlowServiceTest.java
git commit -m "fix(flow): route AGENT node through the reactive path, removing its .block()

GraphFlowService.agentAction called agentService.execute(...), whose .block()
throws on any Reactor non-blocking subscriber thread. AsyncNodeAction already
returns a CompletableFuture, so executeAsync(...).toFuture() drops the blocking
entirely and makes AGENT nodes safe for every consumer.

Adds the regression anchor: an AGENT node backed by a genuinely blocking agent,
subscribed on Schedulers.parallel(), which failed before this change."
```

---

## Task 6: 移植到 feature/1.0.x（受阻，条件执行）

**Files:** 与 Task 1-5 相同的 5 个源文件 + 5 个测试文件

**前置**：spec §9 的残留项——1.0.x 分支的 `ddd4j-ai-samples/pom.xml:66` 与 `ddd4j-ai-sdk-deps/pom.xml:20/25/30` 有依赖缺 version 的既有缺陷，且本机 `~/.m2` 无 1.0.x 产物，需从零构建整条 reactor。**该修复需单独授权（改 pom）。**

- [ ] **Step 1: 确认前置是否解除**

```bash
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/.codex-worktrees/ddd4j-ai-1.0.x 2>/dev/null \
  && mvn -pl ddd4j-ai-extensions/ddd4j-ai-extension-agent -am -Denforcer.skip=true -DskipITs test 2>&1 | tail -20 \
  || echo "worktree 不存在，需先创建"
```

- 若仍报 `ddd4j-ai-extension-document` / `easy4j:*` 缺 version → **停止本 Task**，向用户报告并请求授权修 POM。**不要擅自改 pom。**
- 若构建通过 → 继续 Step 2。

- [ ] **Step 2: 套用补丁（用 `git checkout <branch> -- <paths>` 保证内容恒定）**

```bash
W=/Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/.codex-worktrees/ddd4j-ai-1.0.x
cd $W
A=ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/main/java/io/ddd4j/ai/extension/agent
B=ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/test/java/io/ddd4j/ai/extension/agent
F=ddd4j-ai-extensions/ddd4j-ai-extension-flow/src/main/java/io/ddd4j/ai/extension/flow/service/impl
T=ddd4j-ai-extensions/ddd4j-ai-extension-flow/src/test/java/io/ddd4j/ai/extension/flow/service/impl
# 套用前先确认 1.0.x 与 2.0.x 的基线吻合（应对每个文件 diff 为空）
for f in $A/util/BlockingCallGuard.java $A/service/AgentService.java $A/agent/AgentScopeAgentAdapter.java \
         $A/dispatch/AgentPlanOrchestrator.java $F/GraphFlowService.java; do
  git cat-file -e feature/1.0.x:$f 2>/dev/null && echo "EXISTS-1.0.x $f" || echo "NEW-in-2.0.x $f"
done
git checkout feature/2.0.x -- $A/util/BlockingCallGuard.java $A/service/AgentService.java \
  $A/agent/AgentScopeAgentAdapter.java $A/dispatch/AgentPlanOrchestrator.java $F/GraphFlowService.java \
  $B/util/BlockingCallGuardTest.java $B/service/AgentServiceContractTest.java \
  $B/agent/AgentScopeAgentAdapterTest.java $B/dispatch/AgentPlanOrchestratorTest.java $T/GraphFlowServiceTest.java
git status --short
```

- [ ] **Step 3: 用 Maven 3 构建并测试**

```bash
export JAVA_HOME=/Users/wandl/Library/Java/JavaVirtualMachines/corretto-17.0.20.1/Contents/Home
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/.codex-worktrees/ddd4j-ai-1.0.x
mvn -pl ddd4j-ai-extensions/ddd4j-ai-extension-agent,ddd4j-ai-extensions/ddd4j-ai-extension-flow \
  -Denforcer.skip=true -DskipITs test
```

Expected: 与 2.0.x 相同的用例数全绿。

- [ ] **Step 4: 验证两线关键文件逐字节相同**

```bash
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
A=ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/main/java/io/ddd4j/ai/extension/agent
B=ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/test/java/io/ddd4j/ai/extension/agent
F=ddd4j-ai-extensions/ddd4j-ai-extension-flow/src/main/java/io/ddd4j/ai/extension/flow/service/impl
T=ddd4j-ai-extensions/ddd4j-ai-extension-flow/src/test/java/io/ddd4j/ai/extension/flow/service/impl
for f in \
  $A/util/BlockingCallGuard.java \
  $A/service/AgentService.java \
  $A/agent/AgentScopeAgentAdapter.java \
  $A/dispatch/AgentPlanOrchestrator.java \
  $F/GraphFlowService.java \
  $B/util/BlockingCallGuardTest.java \
  $B/service/AgentServiceContractTest.java \
  $B/agent/AgentScopeAgentAdapterTest.java \
  $B/dispatch/AgentPlanOrchestratorTest.java \
  $T/GraphFlowServiceTest.java ; do
  if git diff --quiet feature/1.0.x:$f feature/2.0.x:$f 2>/dev/null; then
    echo "OK   $f"
  else
    echo "DIFF $f"; git diff feature/1.0.x:$f feature/2.0.x:$f | head -15
  fi
done
```

Expected: 全部 `OK`。

- [ ] **Step 5: 提交并推送两线**

1.0.x worktree 内以 spec 一致的提交信息提交（5 个源文件 + 5 个测试文件），然后：

```bash
git fetch github feature/1.0.x && git fetch origin feature/1.0.x
git push github feature/1.0.x && git push origin feature/1.0.x
```

2.0.x 侧推送 Task 1-5 的提交：

```bash
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
git fetch github feature/2.0.x && git fetch origin feature/2.0.x
git push github feature/2.0.x && git push origin feature/2.0.x
```

推送前必须确认无并行会话分叉；**不加 `-f`**。

---

## 验收清单

- [ ] `BlockingCallGuard` 存在，在 `parallel()` 上抛含替代方案的 `IllegalStateException`，在 `boundedElastic` 与普通线程上放行。
- [ ] `AgentService.executeAsync` 为 `default`，把 `execute` 卸载到 boundedElastic（以线程名断言）。
- [ ] `AgentScopeAgentAdapter.executeAsync` 为零阻塞覆写；`execute` 含守卫；`toResult` 被两条路径共用。
- [ ] `AgentPlanOrchestrator` 有 `dispatchAllAsync` / `mergeResultsAsync`；`executeTask` 无内层 `.block()`；两个同步方法含守卫。
- [ ] **核心证据**：`agentNode_withBlockingAgentService_subscribedOnNonBlockingThread_succeeds` 通过——精确复现 spec §2 F2 的失败条件并证明已修复。
- [ ] 既有同步签名全部未变；第 1 项的 `streamEvents` 测试保持通过。
- [ ] agent 模块与 flow 模块既有测试**零回归**。
- [ ] 未新增 `spring-webflux` 或任何新依赖。
- [ ] 两线同步（1.0.x 受前置阻塞时，先交付 2.0.x 并如实标注）。

## 明确不在本计划范围

- `AgentDispatchTaskRepository` 持久化 → 第 3 项
- TTS / ASR Router 收敛 → 第 4 项
- Micrometer 埋点 → 第 5 项
- 修 1.0.x 的 POM 缺陷（需单独授权）→ 见 Task 6 Step 1
