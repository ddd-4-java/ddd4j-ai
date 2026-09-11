# Agent 细粒度事件流实施计划（streamEvents）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `AgentService` SPI 暴露 Agentscope 细粒度事件流（逐字文本 delta / 思考 delta / 工具调用 delta），并同补丁移植到 `feature/1.0.x`。

**Architecture:** 在 `AgentService` 端口加一个 `default` 方法 `streamEvents(AgentTask)`，默认抛 `UnsupportedOperationException`；唯一实现 `AgentScopeAgentAdapter` 覆写它，直接委托 `harnessAgent.streamEvents(new UserMessage(instruction))` 并把异常映射为 `AgentExecutionException`。**不做事件类型映射**——原生透传 `io.agentscope.core.event.AgentEvent`，30 余种事件类型全量保真。既有 `execute` / `stream` 签名与行为一字不动。

**Tech Stack:** Java 17、Reactor（`Flux`）、AgentScope Java 2.0.2（`agentscope-core` / `agentscope-harness`）、JUnit 5 + AssertJ + Mockito、Maven（2.0.x 用 4.0.0-rc-6 / 1.0.x 用 3.x）。

**Spec:** `docs/superpowers/specs/2026-09-11-agent-fine-grained-event-stream-design.md`

## Global Constraints

以下为 spec 的项目级约束，**每个任务都隐含包含**，逐字生效：

- **Java 17**，两条线均为 `<java.version>17</java.version>`；record / lambda / switch 表达式可用。
- **构建工具按线区分**：`feature/2.0.x` 的 `modelVersion=4.1.0` + `<subprojects>`，**必须**用 Maven 4（`/Users/wandl/tools/apache-maven-4.0.0-rc-6/bin/mvn`，Maven 3 会报 `Malformed POM: Unrecognised tag: 'subprojects'`）；`feature/1.0.x` 的 `modelVersion=4.0.0`，用 Maven 3。
- **JAVA_HOME 必须为 JDK 17**：`/Users/wandl/Library/Java/JavaVirtualMachines/corretto-17.0.20.1/Contents/Home`。系统默认 JDK 是 26 / 21，不设会编译目标错位。
- **本项不改动任何 pom**（spec §7）。两线差异仅在 pom 语法与 `revision`，本项不碰它们。
- **两线的 `AgentService.java` 与 `AgentScopeAgentAdapter.java` 必须保持逐字节相同**（spec §2 F4，移植前实测为空 diff）。移植后须再次用空 diff 验证。
- **1.0.x 的 tts 扩展是旧版**（无 `bridge/`、`chunk/`、`metrics/`、`router/`）——**Task 3 不移植到 1.0.x**。
- **禁止 `git add -A` / `git add .`**：本仓是多会话共用的 worktree，只显式 stage 本任务列出的文件路径。
- 所有测试必须**离线可跑**，不依赖网络、真实 LLM 或 API key。

---

## File Structure

| 文件 | 职责 | 动作 |
|------|------|------|
| `ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/main/java/io/ddd4j/ai/extension/agent/service/AgentService.java` | SPI 端口；新增 `default streamEvents` | 改 |
| `ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/main/java/io/ddd4j/ai/extension/agent/agent/AgentScopeAgentAdapter.java` | 唯一实现；覆写 `streamEvents` 委托 HarnessAgent | 改 |
| `ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/test/java/io/ddd4j/ai/extension/agent/service/AgentServiceContractTest.java` | 端口契约测试（`FakeAgentService` 不覆写新方法，正好测 default） | 改 |
| `ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/test/java/io/ddd4j/ai/extension/agent/agent/AgentScopeAgentAdapterTest.java` | 适配器单测：委托、异常映射、null 校验、顺序保真 | 改 |
| `ddd4j-ai-extensions/ddd4j-ai-extension-tts/src/test/java/io/ddd4j/ai/extension/tts/bridge/TextDeltaTtsBridgeTest.java` | 把 mock 事件换为真实 `TextBlockDeltaEvent`，闭合既有验证缺口 | 改 |

**设计说明（供实施者理解）：**
- 新方法返回 `Flux<io.agentscope.core.event.AgentEvent>`——**细粒度**事件流；既有 `stream()` 返回 `Flux<AgentStep>`（映射自**粗粒度** `io.agentscope.core.agent.Event`）。两者是 Agentscope 的两套并行事件抽象，简单名不同（`Event` vs `AgentEvent`），**不构成 import 冲突**，但同文件共存时需注释说明区别。
- `TextBlockDeltaEvent` 的**3 参构造器已实测确认**为 `(String replyId, String blockId, String delta)`（`javap -c` 显示 `putfield` 顺序：param1→`replyId`、param2→`blockId`、param3→`delta`）。`getDelta()` 返回第三个参数。
- `TextBlockEndEvent` 的构造器**未确认**，因此测试**不使用**它——`TextDeltaTtsBridge.pipe` 的 `doOnComplete` 分支同样会 flush 残余文本，用"流自然结束"即可覆盖。

---

## Task 1: AgentService 新增 default streamEvents

**Files:**
- Modify: `ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/main/java/io/ddd4j/ai/extension/agent/service/AgentService.java`
- Test: `ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/test/java/io/ddd4j/ai/extension/agent/service/AgentServiceContractTest.java`

**Interfaces:**
- Consumes: 既有 `AgentTask`（record，`of(String)` 静态工厂）
- Produces: `AgentService.streamEvents(AgentTask) → Flux<io.agentscope.core.event.AgentEvent>`，未实现时同步抛 `UnsupportedOperationException`（message 含 `streamEvents 未实现`）

- [ ] **Step 1: 写失败测试**

在 `AgentServiceContractTest.java` 的最后一个 `}` 之前追加（该类已有 `FakeAgentService`，它**只实现 `execute` 与 `stream`**，不覆写新方法，因此正好命中 default）：

```java
    @Test
    void streamEvents_defaultImpl_throwsUnsupported() {
        // FakeAgentService 未覆写 streamEvents -> 命中 default 实现，应显式失败而非静默降级
        AgentService service = new FakeAgentService();
        assertThatThrownBy(() -> service.streamEvents(AgentTask.of("task")))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("streamEvents 未实现");
    }
```

- [ ] **Step 2: 运行测试确认失败**

```bash
export JAVA_HOME=/Users/wandl/Library/Java/JavaVirtualMachines/corretto-17.0.20.1/Contents/Home
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
/Users/wandl/tools/apache-maven-4.0.0-rc-6/bin/mvn \
  -pl ddd4j-ai-extensions/ddd4j-ai-extension-agent -am \
  -Denforcer.skip=true -DskipITs test
```

Expected: 编译失败，`找不到符号: 方法 streamEvents(AgentTask)`（方法尚不存在）。

- [ ] **Step 3: 加 default 方法**

在 `AgentService.java` 顶部 import 区加一行（放在既有 `import reactor.core.publisher.Flux;` **之前**，保持字典序）：

```java
import io.agentscope.core.event.AgentEvent;
```

然后在接口体末尾（`Flux<AgentStep> stream(AgentTask task);` 之后）追加：

```java
    /**
     * 细粒度事件流：原生透传 Agentscope {@link AgentEvent}（含逐字文本 delta、
     * 思考 delta、工具调用 delta），供逐字 TTS / 过程可视化等场景消费。
     *
     * <p>与 {@link #stream(AgentTask)} 的区别：{@code stream} 返回经映射的粗粒度
     * {@link AgentStep}（整段文本）；本方法返回未经映射的细粒度事件，保真度更高。
     *
     * <p>默认实现抛 {@link UnsupportedOperationException}，表示该实现不支持细粒度
     * 事件流——不会静默降级为粗粒度输出。
     *
     * @param task 智能体任务
     * @return 细粒度事件流
     */
    default Flux<AgentEvent> streamEvents(AgentTask task) {
        throw new UnsupportedOperationException(
                "streamEvents 未实现：当前 AgentService 实现不支持细粒度事件流");
    }
```

- [ ] **Step 4: 运行测试确认通过**

```bash
export JAVA_HOME=/Users/wandl/Library/Java/JavaVirtualMachines/corretto-17.0.20.1/Contents/Home
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
/Users/wandl/tools/apache-maven-4.0.0-rc-6/bin/mvn \
  -pl ddd4j-ai-extensions/ddd4j-ai-extension-agent -am \
  -Denforcer.skip=true -DskipITs test
```

Expected: `Tests run: 6, Failures: 0, Errors: 0`（`AgentServiceContractTest` 原 5 例 + 新 1 例）。模块全量测试无回归。

- [ ] **Step 5: 提交**

```bash
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
git add \
  ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/main/java/io/ddd4j/ai/extension/agent/service/AgentService.java \
  ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/test/java/io/ddd4j/ai/extension/agent/service/AgentServiceContractTest.java
git commit -m "feat(agent): add default AgentService.streamEvents port method

Exposes Agentscope's fine-grained AgentEvent stream on the SPI as a default
method returning Flux<io.agentscope.core.event.AgentEvent>. Default throws
UnsupportedOperationException so existing third-party implementations stay
source-compatible and the missing capability fails loudly instead of silently
degrading to the coarse-grained stream()."
```

---

## Task 2: AgentScopeAgentAdapter 覆写 streamEvents

**Files:**
- Modify: `ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/main/java/io/ddd4j/ai/extension/agent/agent/AgentScopeAgentAdapter.java`
- Test: `ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/test/java/io/ddd4j/ai/extension/agent/agent/AgentScopeAgentAdapterTest.java`

**Interfaces:**
- Consumes: Task 1 产出的 `AgentService.streamEvents(AgentTask)`；Agentscope `HarnessAgent.streamEvents(Msg) → Flux<AgentEvent>`；既有 `AgentExecutionException(String, Throwable)`
- Produces: 可用的细粒度事件流实现；错误统一包装为 `AgentExecutionException`

- [ ] **Step 1: 写失败测试**

在 `AgentScopeAgentAdapterTest.java` 追加以下 4 个用例。**注意**：该文件已 `import io.agentscope.core.agent.Event`（粗粒度），新用例用**全限定名** `io.agentscope.core.event.AgentEvent` / `io.agentscope.core.event.TextBlockDeltaEvent` 以免读者混淆两套抽象。

同时在文件顶部 import 区补两条静态导入（若尚无）：

```java
import static org.mockito.Mockito.verify;
```

```java
    @Test
    void streamEvents_delegatesToHarnessAgent() {
        HarnessAgent harness = mock(HarnessAgent.class);
        var delta = new io.agentscope.core.event.TextBlockDeltaEvent("r1", "b1", "你好");
        when(harness.streamEvents(any(Msg.class)))
                .thenReturn(Flux.<io.agentscope.core.event.AgentEvent>just(delta));

        AgentScopeAgentAdapter adapter = new AgentScopeAgentAdapter(harness);
        List<io.agentscope.core.event.AgentEvent> events =
                adapter.streamEvents(io.ddd4j.ai.extension.agent.service.AgentTask.of("hi"))
                        .collectList().block();

        assertThat(events).hasSize(1);
        assertThat(((io.agentscope.core.event.TextBlockDeltaEvent) events.get(0)).getDelta())
                .isEqualTo("你好");
        verify(harness).streamEvents(any(Msg.class));
    }

    @Test
    void streamEvents_preservesOrderAndFidelity() {
        HarnessAgent harness = mock(HarnessAgent.class);
        var e1 = new io.agentscope.core.event.TextBlockDeltaEvent("r1", "b1", "你");
        var e2 = new io.agentscope.core.event.TextBlockDeltaEvent("r1", "b1", "好");
        var e3 = new io.agentscope.core.event.TextBlockDeltaEvent("r1", "b1", "！");
        when(harness.streamEvents(any(Msg.class)))
                .thenReturn(Flux.<io.agentscope.core.event.AgentEvent>just(e1, e2, e3));

        AgentScopeAgentAdapter adapter = new AgentScopeAgentAdapter(harness);
        List<io.agentscope.core.event.AgentEvent> events =
                adapter.streamEvents(io.ddd4j.ai.extension.agent.service.AgentTask.of("hi"))
                        .collectList().block();

        // 方案 B 的核心断言：零映射、顺序与实例完全保真
        assertThat(events).containsExactly(e1, e2, e3);
    }

    @Test
    void streamEvents_mapsErrorToAgentExecutionException() {
        HarnessAgent harness = mock(HarnessAgent.class);
        when(harness.streamEvents(any(Msg.class)))
                .thenReturn(Flux.<io.agentscope.core.event.AgentEvent>error(
                        new IllegalStateException("streamEvents down")));

        AgentScopeAgentAdapter adapter = new AgentScopeAgentAdapter(harness);

        assertThatThrownBy(() -> adapter.streamEvents(
                        io.ddd4j.ai.extension.agent.service.AgentTask.of("hi"))
                .collectList().block())
                .isInstanceOf(AgentExecutionException.class)
                .hasMessageContaining("streamEvents down");
    }

    @Test
    void streamEvents_nullTask_throwsNpe() {
        AgentScopeAgentAdapter adapter =
                new AgentScopeAgentAdapter(mock(HarnessAgent.class));

        assertThatThrownBy(() -> adapter.streamEvents(null))
                .isInstanceOf(NullPointerException.class);
    }
```

- [ ] **Step 2: 运行测试确认失败**

```bash
export JAVA_HOME=/Users/wandl/Library/Java/JavaVirtualMachines/corretto-17.0.20.1/Contents/Home
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
/Users/wandl/tools/apache-maven-4.0.0-rc-6/bin/mvn \
  -pl ddd4j-ai-extensions/ddd4j-ai-extension-agent -am \
  -Denforcer.skip=true -DskipITs test
```

Expected: 编译失败，`找不到符号: 方法 streamEvents(...)`（`FakeAgentService` 走 default，但 adapter 尚未覆写，新用例调用的仍是抛异常的 default——若编译通过则会 4 例全红）。

- [ ] **Step 3: 覆写实现**

在 `AgentScopeAgentAdapter.java` 的 import 区加（与既有 `import io.agentscope.core.agent.Event;` 并列，两者简单名不同）：

```java
import io.agentscope.core.event.AgentEvent;
```

在类体末尾（既有 `extractText` 之后、类闭合 `}` 之前）追加：

```java
    /**
     * 细粒度事件流：直接委托 Agentscope {@link HarnessAgent#streamEvents}，
     * 原生透传 {@link AgentEvent}（逐字 delta / 思考 delta / 工具调用 delta）。
     *
     * <p>与 {@link #stream(AgentTask)} 的粗粒度 {@code io.agentscope.core.agent.Event}
     * 是两套并行抽象：本方法不做事件类型映射，保真度最高。
     */
    @Override
    public Flux<AgentEvent> streamEvents(AgentTask task) {
        Objects.requireNonNull(task, "task");
        return harnessAgent.streamEvents(new UserMessage(task.instruction()))
                .onErrorMap(RuntimeException.class, e -> {
                    log.warn("agentscope streamEvents failed: {}", e.getMessage());
                    return new AgentExecutionException(
                            "agentscope streamEvents failed: " + e.getMessage(), e);
                });
    }
```

- [ ] **Step 4: 运行测试确认通过**

```bash
export JAVA_HOME=/Users/wandl/Library/Java/JavaVirtualMachines/corretto-17.0.20.1/Contents/Home
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
/Users/wandl/tools/apache-maven-4.0.0-rc-6/bin/mvn \
  -pl ddd4j-ai-extensions/ddd4j-ai-extension-agent -am \
  -Denforcer.skip=true -DskipITs test
```

Expected: `AgentScopeAgentAdapterTest` 原 7 例 + 新 4 例 = 11 例全绿；`AgentServiceContractTest` 6 例全绿；模块全量无回归。

- [ ] **Step 5: 核实 streamEvents 是否为阻塞包装（spec §9 风险）**

```bash
export JAVA_HOME=/Users/wandl/Library/Java/JavaVirtualMachines/corretto-17.0.20.1/Contents/Home
javap -c -p -cp ~/.m2/repository/io/agentscope/agentscope-harness/2.0.2/agentscope-harness-2.0.2.jar \
  io.agentscope.harness.agent.HarnessAgent 2>/dev/null | grep -A 25 "streamEvents"
```

Expected: 方法体为 reactive 链，**不含** `block()` 或 `subscribe()`。

若发现阻塞调用：在 Step 6 的提交信息中如实记录该事实，并作为**第 2 项**（阻塞调用隔离到 `boundedElastic`）的输入。**不在本任务修复**——那是第 2 项的范围。

- [ ] **Step 6: 提交**

```bash
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
git add \
  ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/main/java/io/ddd4j/ai/extension/agent/agent/AgentScopeAgentAdapter.java \
  ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/test/java/io/ddd4j/ai/extension/agent/agent/AgentScopeAgentAdapterTest.java
git commit -m "feat(agent): implement streamEvents in AgentScopeAgentAdapter

Delegates to HarnessAgent.streamEvents(Msg) and passes the Agentscope
AgentEvent stream through unmapped, so all ~30 fine-grained event kinds
(TEXT_BLOCK_DELTA / THINKING_BLOCK_DELTA / TOOL_CALL_DELTA / ...) stay
available without enumeration. Runtime failures are wrapped into
AgentExecutionException, matching the existing stream() behaviour."
```

---

## Task 3: tts 桥接改用真实 TextBlockDeltaEvent

**Files:**
- Modify: `ddd4j-ai-extensions/ddd4j-ai-extension-tts/src/test/java/io/ddd4j/ai/extension/tts/bridge/TextDeltaTtsBridgeTest.java`

**Interfaces:**
- Consumes: `TextDeltaTtsBridge.pipe(Flux<io.agentscope.core.event.AgentEvent>, String)`（已存在）；`TextChunker.defaultChunker()`；`TextBlockDeltaEvent(String, String, String)`（参数为 `replyId, blockId, delta`，**已由字节码实测确认**）
- Produces: 对真实 Agentscope 事件类型的保真断言（替代原先对 Mockito mock 的断言）

**为什么这个任务存在**：原 `TextDeltaTtsBridgeTest` 用 `mock(TextBlockDeltaEvent.class)` 并 `when(ev.getDelta()).thenReturn("...")`——它断言的是"我假定的该类型行为"，而非真实行为。若真实类型的 delta 取值语义与假定不符，测试仍会通过而生产失败。本任务用真实构造的事件闭合该缺口。**无需任何 pom 改动**：`agentscope-core` 在本模块是 `provided` 作用域，compile 与 test 期均可见。

- [ ] **Step 1: 写失败测试（真实事件版）**

在 `TextDeltaTtsBridgeTest.java` 末尾追加（该类已有 `audio(String)` 私有助手返回 `byte[]`）：

```java
    @Test
    void pipe_realTextBlockDeltaEvent_feedsChunkerCorrectly() {
        // 用真实 TextBlockDeltaEvent（非 Mockito mock）验证该类型的 getDelta()
        // 与 TextChunker 的期望一致——mock 版只能断言我们的假定。
        // 3 参构造器语义已由 javap -c 确认：(replyId, blockId, delta)
        TextChunker chunker = TextChunker.defaultChunker();
        TtsService tts = mock(TtsService.class);
        when(tts.streamSynthesize(eq("你好。"), any()))
                .thenReturn(Flux.just(audio("real-1")));

        TextDeltaTtsBridge bridge = new TextDeltaTtsBridge(chunker, tts);

        // 句末标点触发 feed() 切分；流自然结束走 doOnComplete 的 flush 分支
        // （不使用 TextBlockEndEvent——其构造器未确认）
        Flux<AgentEvent> events = Flux.<AgentEvent>just(
                new io.agentscope.core.event.TextBlockDeltaEvent("r1", "b1", "你好。"));

        StepVerifier.create(bridge.pipe(events, null))
                .expectNextMatches(arr -> java.util.Arrays.equals(arr, audio("real-1")))
                .expectComplete()
                .verify();

        verify(tts, atLeastOnce()).streamSynthesize(eq("你好。"), eq(null));
    }
```

若文件缺少 `io.agentscope.core.event.AgentEvent` 的 import，则在 import 区补：

```java
import io.agentscope.core.event.AgentEvent;
```

- [ ] **Step 2: 运行测试**

```bash
export JAVA_HOME=/Users/wandl/Library/Java/JavaVirtualMachines/corretto-17.0.20.1/Contents/Home
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
/Users/wandl/tools/apache-maven-4.0.0-rc-6/bin/mvn \
  -pl ddd4j-ai-extensions/ddd4j-ai-extension-tts -am \
  -Denforcer.skip=true -DskipITs test
```

Expected: PASS（`TextDeltaTtsBridgeTest` 原 8 例 + 新 1 例 = 9 例）。

**若失败**：说明真实 `TextBlockDeltaEvent` 的行为与 `TextDeltaTtsBridge` 的假定不符——这是本任务的价值所在。**不要改测试去迁就**；记录实际行为差异，检查 `TextDeltaTtsBridge.pipe` 中对 `TextBlockDeltaEvent` 的处理（`delta.getDelta()` 取值、`TextChunker.feed` 的调用），定位是生产代码问题还是构造参数顺序问题，修正后重跑。

- [ ] **Step 3: 提交**

```bash
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
git add \
  ddd4j-ai-extensions/ddd4j-ai-extension-tts/src/test/java/io/ddd4j/ai/extension/tts/bridge/TextDeltaTtsBridgeTest.java
git commit -m "test(tts): assert TextDeltaTtsBridge against a real TextBlockDeltaEvent

The existing bridge tests stubbed TextBlockDeltaEvent with Mockito, so they
asserted our assumption of the type's behaviour rather than the type itself.
This adds a case built on a real event (3-arg constructor verified by bytecode:
replyId, blockId, delta), closing that gap without any pom change since
agentscope-core is provided scope in this module."
```

---

## Task 4: 移植 Task 1+2 到 feature/1.0.x

**Files:**
- Create（worktree）: 独立 worktree，checkout `feature/1.0.x`
- Modify: 与 Task 1+2 相同的 **4 个文件**（agent 扩展的 2 个源文件 + 2 个测试文件）
- **不改** tts 扩展任何文件（1.0.x 的 tts 是旧版，无 `bridge/` 包）

**Interfaces:**
- Consumes: Task 1+2 在 `feature/2.0.x` 上已验证的补丁
- Produces: `feature/1.0.x` 上等价的实现与测试，两线关键文件保持逐字节相同

- [ ] **Step 1: 用 using-git-worktrees 技能创建隔离 worktree**

**必须先读并遵循 `superpowers:using-git-worktrees`**。不得在当前共享 worktree 上直接 `git checkout feature/1.0.x`——本仓是多会话共用的 worktree（memory 有"他人 WIP 被卷入""worktree 被外部删除"等历史事故），切分支会打断其他会话。

创建后在该 worktree 内确认基线：

```bash
git branch --show-current          # 期望 feature/1.0.x
grep -E "java.version|modelVersion" pom.xml
# 期望 modelVersion 4.0.0、java.version 17
```

- [ ] **Step 2: 套用补丁**

把 Task 1+2 对以下 4 个文件的改动**原样**应用到 1.0.x worktree（因 spec §2 F4 已实测这 4 个文件两线逐字节相同，补丁应无冲突）：

```
ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/main/java/io/ddd4j/ai/extension/agent/service/AgentService.java
ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/main/java/io/ddd4j/ai/extension/agent/agent/AgentScopeAgentAdapter.java
ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/test/java/io/ddd4j/ai/extension/agent/service/AgentServiceContractTest.java
ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/test/java/io/ddd4j/ai/extension/agent/agent/AgentScopeAgentAdapterTest.java
```

- [ ] **Step 3: 用 Maven 3 构建并测试**

```bash
export JAVA_HOME=/Users/wandl/Library/Java/JavaVirtualMachines/corretto-17.0.20.1/Contents/Home
cd <1.0.x worktree 路径>
mvn -pl ddd4j-ai-extensions/ddd4j-ai-extension-agent -am \
  -Denforcer.skip=true -DskipITs test
```

Expected: 与 2.0.x 相同的用例数全绿（`AgentServiceContractTest` 6 例、`AgentScopeAgentAdapterTest` 11 例），模块全量无回归。

- [ ] **Step 4: 验证两线关键文件逐字节相同**

```bash
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
A=ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/main/java/io/ddd4j/ai/extension/agent
B=ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/test/java/io/ddd4j/ai/extension/agent
for f in $A/service/AgentService.java $A/agent/AgentScopeAgentAdapter.java \
         $B/service/AgentServiceContractTest.java $B/agent/AgentScopeAgentAdapterTest.java; do
  if git diff --quiet feature/1.0.x:$f feature/2.0.x:$f 2>/dev/null; then
    echo "OK  $f"
  else
    echo "DIFF $f"; git diff feature/1.0.x:$f feature/2.0.x:$f | head -20
  fi
done
```

Expected: 四行全部 `OK`（无 DIFF）。若有 DIFF，逐条查明原因后修正，不得留差异。

- [ ] **Step 5: 提交并推送两线**

1.0.x worktree：

```bash
cd <1.0.x worktree 路径>
git add \
  ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/main/java/io/ddd4j/ai/extension/agent/service/AgentService.java \
  ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/main/java/io/ddd4j/ai/extension/agent/agent/AgentScopeAgentAdapter.java \
  ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/test/java/io/ddd4j/ai/extension/agent/service/AgentServiceContractTest.java \
  ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/test/java/io/ddd4j/ai/extension/agent/agent/AgentScopeAgentAdapterTest.java
git commit -m "feat(agent): expose fine-grained AgentEvent stream on AgentService (streamEvents)

Same patch as feature/2.0.x — the two branches share byte-identical
AgentService.java and AgentScopeAgentAdapter.java, so this ports without
divergence. Adds a default streamEvents port method and its
AgentScopeAgentAdapter implementation delegating to
HarnessAgent.streamEvents(Msg)."
```

推送前先 fetch 确认无并行会话新提交，再推两个远端（**不加 `-f`**）：

```bash
git fetch github feature/1.0.x && git fetch origin feature/1.0.x
git log --oneline -1 github/feature/1.0.x   # 确认基线，无意外分叉
git push github feature/1.0.x
git push origin feature/1.0.x
```

`feature/2.0.x` 侧同样推送 Task 1+2+3 的 3 个提交：

```bash
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
git fetch github feature/2.0.x && git fetch origin feature/2.0.x
git push github feature/2.0.x
git push origin feature/2.0.x
```

---

## 验收清单（全部满足才算完成）

- [ ] `AgentService.streamEvents` 存在且为 `default`，未实现时同步抛 `UnsupportedOperationException`，message 含 `streamEvents 未实现`。
- [ ] `AgentScopeAgentAdapter.streamEvents` 覆写并委托 `harnessAgent.streamEvents(Msg)`，异常包装为 `AgentExecutionException`，`task` 为 null 时抛 NPE。
- [ ] 新测试用例全部通过：`AgentServiceContractTest` 6 例、`AgentScopeAgentAdapterTest` 11 例、`TextDeltaTtsBridgeTest` 9 例。
- [ ] `execute` / `stream` 的既有测试**零改动零回归**，方法签名未变。
- [ ] `TextDeltaTtsBridgeTest` 中至少一个用例使用**真实** `TextBlockDeltaEvent`。
- [ ] 两条线各自构建 + 测试通过（2.0.x 用 Maven 4，1.0.x 用 Maven 3）。
- [ ] 两线的 4 个关键文件 `git diff` 为空（逐字节相同）。
- [ ] 两线均已推送 GitHub + Codeup（无 force）。
- [ ] 全程未改动任何 `pom.xml`。

## 明确不在本计划范围

- Flow 的 AGENT 节点流式化、TTS 节点类型 → 第 4 项（TTS/ASR Router 收敛）
- `AgentScopeAgentAdapter.execute` / `AgentPlanOrchestrator.dispatchAll` 的 `.block()` 隔离 → 第 2 项
- `AgentDispatchTaskRepository` 持久化 → 第 3 项
- Micrometer 可观测性 → 第 5 项
- codegraph 排除规则、死代码清理 → 第 6 项
