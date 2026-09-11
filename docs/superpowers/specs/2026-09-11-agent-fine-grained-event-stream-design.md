# Agent 细粒度事件流设计（streamEvents）

- 日期：2026-09-11
- 作者：PartMe.AI
- 状态：待实施（设计已评审通过）
- 范围：`ddd4j-ai-extension-agent` —— 在 `AgentService` SPI 暴露 Agentscope 细粒度事件流
- 分支：`feature/2.0.x`（实施与验证）+ `feature/1.0.x`（同补丁移植）
- 关联文档：`2026-08-12-agent-component-design.md`（agent 组件设计）、`2026-08-07-ddd4j-ai-architecture-design.md`（整体架构）
- 本文件是「ddd4j-ai 优化改造」6 项序列中的**第 1 项**；后续 5 项各自独立走 spec → plan → 实施循环

---

## 1. 背景

`AgentScopeAgentAdapter` 当前只消费 Agentscope 的粗粒度事件：`HarnessAgent.stream(Msg)` 返回 `Flux<io.agentscope.core.agent.Event>`，其 `EventType` 仅有 5 个值（`REASONING` / `TOOL_RESULT` / `HINT` / `AGENT_RESULT` / `SUMMARY`），且 `event.getMessage().getTextContent()` 拿到的是**已经拼接完成的整段文本**。

这构成所有流式体验的上游瓶颈：

| 上游限制 | 下游无法实现 |
|----------|-------------|
| 拿不到逐字 delta | 真正的逐字 TTS（`TextDeltaTtsBridge` 只能用 mock 事件测试，无法验证真实 delta 行为） |
| 拿不到 thinking delta | CoT 过程可视化 |
| 拿不到 tool call delta | 工具调用参数的增量回显 |
| 拿不到 tool result delta | 长工具输出的流式展示 |

而当前依赖的 `agentscope-core 2.0.2` 已提供 `HarnessAgent.streamEvents(...)`，返回 `Flux<io.agentscope.core.event.AgentEvent>`，事件类型覆盖 `TEXT_BLOCK_START/DELTA/END`、`THINKING_BLOCK_*`、`TOOL_CALL_START/DELTA/END`、`TOOL_RESULT_START/TEXT_DELTA/DATA_DELTA/END`、`MODEL_CALL_*`、`AGENT_START/END` 等 30 余种。本次改造把这个能力经 SPI 暴露出来。

## 2. 关键事实（实证核实）

以下全部经 `git` / `javap` / `jar tf` 实测，非推断：

**F1. ddd4j-ai 只有 2 条 feature 分支**
`git branch -a` 输出：`feature/1.0.x`、`feature/2.0.x`、`feature/sdk-deps`、`master`。**不存在 `feature/3.0.x`**。
（注意：ddd4j 主仓、ddd4j-boot、rocketmq-extension 均为三线，"三线"是生态惯例而非本仓事实。）

**F2. 两条线的 agentscope 版本相同**
`git show <branch>:ddd4j-ai-dependencies/pom.xml` 两线均为 `<agentscope-java.version>2.0.2</agentscope-java.version>`。
→ 方案 B 依赖的事件模型两线一致，**移植风险为 0**。

**F3. 两条线均为 Java 17**
两线根 pom 均为 `<java.version>17</java.version>` → record / lambda / switch 表达式均可用。

**F4. 关键源文件两线逐字节相同**
`git diff feature/1.0.x:<path> feature/2.0.x:<path>` 对以下两文件**输出为空**：
- `ddd4j-ai-extension-agent/.../service/AgentService.java`
- `ddd4j-ai-extension-agent/.../agent/AgentScopeAgentAdapter.java`

→ 同一份补丁可直接套用两端。

**F5. 两线差异仅在 pom 语法与版本号**
| 项 | feature/1.0.x | feature/2.0.x |
|----|---------------|---------------|
| `modelVersion` | 4.0.0（Maven 3） | 4.1.0 + `<subprojects>`（Maven 4） |
| `revision` | `1.0.x.20260630-SNAPSHOT` | `2.0.x.20260630-SNAPSHOT` |

**F6. 两种事件类型名称近似，易混淆（重要）**
`agentscope-core 2.0.2` 同时存在：
- `io.agentscope.core.agent.Event` —— 粗粒度，`stream(Msg)` 返回，当前 adapter 已 import
- `io.agentscope.core.event.AgentEvent` —— 细粒度，`streamEvents(...)` 返回，本次新增

两者可同时出现在同一文件里。实施时必须避免 import 冲突（见 §6）。

**F7. `streamEvents` 有 4 个重载**
```
Flux<AgentEvent> streamEvents(Msg)
Flux<AgentEvent> streamEvents(List<Msg>)
Flux<AgentEvent> streamEvents(Msg, RuntimeContext)
Flux<AgentEvent> streamEvents(String)
```

**F8. `agentscope-core` 在 agent 扩展中是 `compile` 作用域**
`ddd4j-ai-extension-agent/pom.xml` 注释明确：「AgentScope 集成（compile scope：作为默认智能体实现暴露给业务调用方）」。
→ 把 `AgentEvent` 放上 SPI **不需要改动任何依赖声明**，业务方本来就已通过传递依赖拿到该类型。

**F9. 两个目标测试类已存在**
- `src/test/java/io/ddd4j/ai/extension/agent/service/AgentServiceContractTest.java`
- `src/test/java/io/ddd4j/ai/extension/agent/agent/AgentScopeAgentAdapterTest.java`

→ 扩展既有测试，不新建测试类。

## 3. 目标

- 在 `AgentService` SPI 暴露细粒度事件流，业务方可经接口（而非 `HarnessAgent` 具体类）消费逐字 delta。
- 保持 `execute` / `stream` 两个既有方法**签名与行为完全不变**。
- 对既有第三方 `AgentService` 实现保持**二进制与源码兼容**。
- 同补丁移植到 `feature/1.0.x`。

### 非目标

- **不做事件类型映射**。方案 B 直接透传 Agentscope 的 `AgentEvent`，不建自有事件模型（详见 §4 取舍）。
- **不改 Flow 的 AGENT 节点**。节点流式化与 TTS 节点类型属于第 4 项（TTS/ASR Router 收敛）的范围。
- **不移除 tts 扩展对 `agentscope-core` 的 provided 依赖**。该收益只属于方案 A；方案 B 下 `TextDeltaTtsBridge` 的签名仍需该类型，依赖必须保留。
- 不引入自动装配变更（`AgentAutoConfiguration` 不动）。

## 4. 关键决策

### 决策 1：暴露 Agentscope 原生 `AgentEvent`（方案 B）

三个候选及其取舍：

| 方案 | 做法 | 优势 | 代价 |
|------|------|------|------|
| A | 新建 ddd4j 自有 `AgentStreamEvent` record + 映射层 | SPI 框架无关；三线换引擎也能实现；**可移除 tts→agentscope 依赖** | 多一个记录类型 + 映射层；Agentscope 新增事件类型须扩枚举 |
| **B（选定）** | SPI 直接返回 `Flux<io.agentscope.core.event.AgentEvent>` | 零映射、100% 保真、`TextDeltaTtsBridge` 零改动 | SPI 永久耦合 agentscope；业务方编译期须能拿到该类型 |
| C | 自有类型 + `raw()` 逃生舱 | 常用路径类型安全 | 类型设计复杂度换来的干净性名存实亡 |

**选 B 的理由**：`ddd4j-ai-extension-agent` 的定位本身就是"Agentscope 集成"——实现类名为 `AgentScopeAgentAdapter`，pom 以 `compile` 作用域硬依赖 `agentscope-harness`，且原自研 ReAct/Plan-Execute 已被删除（基线快照仅存于 `docs/superpowers/plans/baseline-*.java`）。在这个前提下拉一个"框架无关"的抽象层，是为不存在的多引擎未来付维护成本（YAGNI）。B 同时让 30 余种事件类型全量可用，无需枚举全集，也免去了 Agentscope 演进时的同步负担。

**已知代价的接受**：SPI 与 agentscope 版本绑定。若未来需要换引擎，届时再引入 A 的映射层——因为 B 没有破坏现有方法，迁移路径是开放的。

### 决策 2：新方法用 `default` 实现，默认抛 `UnsupportedOperationException`

```java
default Flux<AgentEvent> streamEvents(AgentTask task) {
    throw new UnsupportedOperationException(
            "streamEvents 未实现：当前 AgentService 实现不支持细粒度事件流");
}
```

理由：`AgentService` 是公开 SPI，业务方可能已有自定义实现。若声明为 `abstract`，所有既有实现会**编译失败**（源码不兼容）。用 `default` 则：
- 既有实现无需改动即可编译通过
- 调用方若命中未实现的 impl，得到**显式异常**而非静默退化
- `AgentScopeAgentAdapter` 覆写它成为唯一支持者

选"显式抛异常"而非"返回空 Flux"：空流会让调用方误以为"智能体没有输出"，把契约缺失伪装成业务结果，是最糟的失败模式。

### 决策 3：两线同步，同补丁移植

基于 F2/F4，1.0.x 与 2.0.x 的 `AgentService.java`、`AgentScopeAgentAdapter.java` 逐字节相同，因此：
- 先在 `feature/2.0.x` 实施 + 验证（Maven 4 构建）
- 确认绿后，把同一份改动应用到 `feature/1.0.x`（Maven 3 构建）
- 两线各自独立提交、独立推送

## 5. 接口方向

```java
// AgentService.java —— 新增 default 方法；execute / stream 一字不动
package io.ddd4j.ai.extension.agent.service;

import io.agentscope.core.event.AgentEvent;
import reactor.core.publisher.Flux;

public interface AgentService {

    AgentResult execute(AgentTask task) throws Exception;      // 既有，不改

    Flux<AgentStep> stream(AgentTask task);                    // 既有，不改

    /**
     * 细粒度事件流：透传 Agentscope {@link AgentEvent}（含逐字文本 delta、
     * 思考 delta、工具调用 delta），供逐字 TTS / 过程可视化等场景消费。
     *
     * <p>默认实现抛 {@link UnsupportedOperationException}，表示该实现不支持细粒度事件流
     * （不会静默降级为 {@link #stream(AgentTask)} 的粗粒度输出）。
     *
     * @param task 智能体任务
     * @return 事件流
     */
    default Flux<AgentEvent> streamEvents(AgentTask task) {
        throw new UnsupportedOperationException(
                "streamEvents 未实现：当前 AgentService 实现不支持细粒度事件流");
    }
}
```

```java
// AgentScopeAgentAdapter.java —— 覆写 + 既有 toStep/extractText 不动
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

**错误处理**：与既有 `stream()` 保持一致——`RuntimeException` 统一包装为 `AgentExecutionException`（该异常已存在于 `agent/AgentExecutionException.java`）。

**数据流**：
```
AgentTask.instruction
  → new UserMessage(instruction)
  → harnessAgent.streamEvents(...)          // Flux<io.agentscope.core.event.AgentEvent>
  → onErrorMap(RuntimeException → AgentExecutionException)
  → 调用方（业务 / TextDeltaTtsBridge / 前端 SSE）
```

## 6. 模块落点

| 文件 | 动作 | 说明 |
|------|------|------|
| `ddd4j-ai-extension-agent/src/main/java/io/ddd4j/ai/extension/agent/service/AgentService.java` | 改 | 加 import + `default streamEvents` |
| `ddd4j-ai-extension-agent/src/main/java/io/ddd4j/ai/extension/agent/agent/AgentScopeAgentAdapter.java` | 改 | 加 import + 覆写 `streamEvents` |
| `.../src/test/java/.../service/AgentServiceContractTest.java` | 改 | 加 default 方法契约用例 |
| `.../src/test/java/.../agent/AgentScopeAgentAdapterTest.java` | 改 | 加委托与异常映射用例 |
| `ddd4j-ai-extension-tts/src/test/java/io/ddd4j/ai/extension/tts/bridge/TextDeltaTtsBridgeTest.java` | 改 | 把 mock 事件换成**真实** `TextBlockDeltaEvent`（见 §8 验收 2） |

**import 冲突处理（对应 F6）**：`AgentScopeAgentAdapter` 已 `import io.agentscope.core.agent.Event`（粗粒度）。新增细粒度事件时建议**不 import** `io.agentscope.core.event.AgentEvent`，而在方法签名中写全限定名，或反之——须保证同文件内两个近似类型名不产生阅读歧义。实施时二选一并加注释说明。

## 7. 依赖

**无新增依赖。** 依据 F8，`agentscope-core` 在 agent 扩展中已是 `compile` 作用域，`io.agentscope.core.event.AgentEvent` 随其可用；业务方经传递依赖亦已可获得。两线 pom 均无需改动。

## 8. 测试策略

遵循项目既有的"契约测试 + 适配器测试"双层约定，全部离线可跑（不依赖网络 / 不依赖真实 LLM）。

| 测试类 | 新增用例 | 断言要点 |
|--------|---------|---------|
| `AgentServiceContractTest` | `streamEvents_defaultImpl_throwsUnsupported` | 匿名实现不覆写该 default 时，调用抛 `UnsupportedOperationException` 且 message 含 "streamEvents 未实现" |
| `AgentScopeAgentAdapterTest` | `streamEvents_delegatesToHarnessAgent` | mock `HarnessAgent`，`streamEvents(task)` 触发 `harnessAgent.streamEvents(...)` 一次；返回事件原样透传 |
| `AgentScopeAgentAdapterTest` | `streamEvents_mapsErrorToAgentExecutionException` | mock 使其返回 `Flux.error(new IllegalStateException("boom"))`，订阅后得到 `AgentExecutionException` 且 cause 为原异常 |
| `AgentScopeAgentAdapterTest` | `streamEvents_nullTask_throwsNpe` | 传 null 抛 `NullPointerException`（与 `execute`/`stream` 的 `requireNonNull` 一致） |
| `AgentScopeAgentAdapterTest` | `streamEvents_preservesOrderAndFidelity` | mock 返回 3 个预设事件，验证顺序与实例/字段完全一致（方案 B 的"零映射保真"是核心断言） |

### 验收标准

1. **单测**：上述 5 个用例通过，且 `${module}` 全量测试无回归。
2. **真实事件保真验证**（本项的核心价值，同时修掉一处既有验证缺口）：
   - 现有 `TextDeltaTtsBridgeTest` 用的是 **Mockito mock** 的 `TextBlockDeltaEvent`——它断言的是"我假定的 `getDelta()` 行为"，而非该类型的真实行为。这是一处真实缺口：若真实事件类的 delta 取值方式与假定不符，测试仍会通过而生产会失败。
   - 将该测试中至少一个用例改用**真实构造**的 `TextBlockDeltaEvent`（有 3 参与 5 参两个构造器，实施时先 `javap` 确认参数语义），验证真实类型的 `getDelta()` 与 `TextChunker` 的期望一致。
   - **无需任何 pom 改动**：`agentscope-core` 在 tts 模块是 `provided` 作用域，compile 与 test 期均可见。
   - 两半之间的类型兼容性由编译器保证（`AgentService.streamEvents` 返回 `Flux<io.agentscope.core.event.AgentEvent>`，`TextDeltaTtsBridge.pipe` 接收同一类型），无需额外测试。
3. **向后兼容**：`execute` / `stream` 的既有测试全部保持通过，无任何签名变更。
4. **两线**：`feature/2.0.x` 与 `feature/1.0.x` 各自构建 + 测试通过，各自推送 GitHub + Codeup。

### 构建与验证命令（两线不同）

```bash
# feature/2.0.x —— Maven 4 + JDK 17（modelVersion 4.1.0，Maven 3 会报 Malformed POM）
export JAVA_HOME=/Users/wandl/Library/Java/JavaVirtualMachines/corretto-17.0.20.1/Contents/Home
/Users/wandl/tools/apache-maven-4.0.0-rc-6/bin/mvn \
  -pl ddd4j-ai-extensions/ddd4j-ai-extension-agent -am \
  -Denforcer.skip=true -DskipITs test

# feature/1.0.x —— Maven 3 + JDK 17（modelVersion 4.0.0）
mvn -pl ddd4j-ai-extensions/ddd4j-ai-extension-agent -am \
  -Denforcer.skip=true -DskipITs test
```

## 9. 风险与缓解

| 风险 | 等级 | 缓解 |
|------|------|------|
| `streamEvents(...)` 内部可能是 `.block()` 包装，导致在 WebFlux 事件循环线程上行为异常 | 中 | 实施时读 `agentscope-harness` 反编译确认；若确有阻塞，记录下来并纳入**第 2 项**（阻塞调用隔离到 boundedElastic）的范围 |
| 4 个重载如何选择不明确（是否需要 `RuntimeContext`） | 低 | 先用 `streamEvents(Msg)` 最小重载；`RuntimeContext` 与多轮记忆/状态存储相关，留待有实际需求时再暴露 |
| 同文件两个近似事件类型名造成误读 | 低 | 见 §6：一处用全限定名 + 注释说明，并在 Javadoc 标注两者区别 |
| 新增 `default` 方法对业务自定义实现产生"运行期才发现未实现" | 低 | 显式 `UnsupportedOperationException` + message 指引；不静默降级 |
| `TextBlockDeltaEvent` 的构造参数语义未知，可能写错真实事件用例 | 低 | 实施时先 `javap -p` 确认 3 参与 5 参构造器的参数含义，再写用例 |
| 1.0.x 移植时误用 Maven 4 专用语法 | 低 | 两线 pom 语法不同（F5）；本次改动**不涉及 pom**，风险实际为 0 |

## 10. 后续项

本项完成后，6 项序列的其余 5 项各自独立走 spec → plan → 实施：

2. 阻塞调用隔离到 `boundedElastic`（`AgentScopeAgentAdapter.execute` / `AgentPlanOrchestrator.dispatchAll` 的 `.block()`）
3. `AgentDispatchTaskRepository` 持久化实现（当前仅 `InMemoryAgentDispatchTaskRepository`）
4. TTS / ASR 双 Router 收敛（Azure TTS 适配为 `TtsService` 第三后端；ASR 侧补统一端口）
5. Micrometer 可观测性专项（引入 actuator，各扩展 `ObjectProvider<MeterRegistry>` 可选注入）
6. 工程卫生批次（codegraph 排除 `docs/**` 与 `baseline-*.java`、删 `FallbackTtsRouter.of()` 与 `TextDeltaTtsBridge.ignored()`、`@Tag("network")` CI job）
