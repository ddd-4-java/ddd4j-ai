# AI Core 契约接口设计

- 日期：2026-07-01
- 作者：PartMe.AI
- 状态：**已实现**（commit `2b808dc` feat(ai-core)）
- 范围：`ddd4j-ai-core` —— AI 通用纯 Java 契约层
- 关联文档：整体架构与分层约定见 `2026-08-07-ddd4j-ai-architecture-design.md`

---

## 1. 背景

在 sst 语音组件（`2026-01-05`）先行落地后，发现各 AI 组件缺少统一的稳定命名与调用契约，导致路由、诊断、配置定位无公共基线。`ddd4j-ai-core` 于 `2026-07-01` 反向抽取，提供不绑定任何框架（纯 Java）的契约层，作为所有 `ddd4j-ai-extension-*` 的公共依赖。

## 2. 目标

- 提供所有 AI 组件的**稳定命名契约**（`AiComponent`）。
- 提供最小化、框架无关的**调用契约**（`AiHandler` + `AiRequest`/`AiResponse`）。
- 保证请求/响应**不可变**，便于并发与缓存。

### 非目标

- 不定义具体业务语义（对话、嵌入、语音等语义由各 extension 定义）。
- 不引入 Spring 或任何运行时框架依赖（pom 不含 Spring）。
- 不规定异步/流式协议（流式响应由各组件自行扩展，未来可在此层追加）。

## 3. 关键决策

| # | 决策 | 理由 |
|---|------|------|
| C1 | 契约层为**纯 Java**，pom 零框架依赖 | 可在任意宿主复用，版本最稳定，升级成本最低 |
| C2 | `AiHandler extends AiComponent` | 可调用处理器天然需要稳定命名（路由/诊断），继承避免双接口与类型转换 |
| C3 | `AiRequest`/`AiResponse` 使用 **Java record** | 不可变、equals/hashCode/toString 免写、与未来值类演进兼容 |
| C4 | 紧凑构造器做**防御性不可变**：`input`/`output` `requireNonNull`，`metadata` null→`Map.of()`，非空→`Map.copyOf` | 杜绝 null 与外部可变集合注入，保证线程安全 |
| C5 | 提供 `of(String)` 静态工厂 | 无 metadata 场景的最常用入口，降低样板代码 |
| C6 | `metadata` 类型为 `Map<String, Object>` | 兼顾灵活（任意诊断/链路/供应商字段）与简单（不引入强类型 schema） |

## 4. 总体架构

```
io.ddd4j.ai.core
├── AiComponent        (interface)   稳定命名
├── AiHandler          (interface extends AiComponent)   调用契约
├── AiRequest          (record)      不可变请求
├── AiResponse         (record)      不可变响应
└── package-info                     包文档
```

依赖方向：`extension-*` → `core`。core 不依赖任何其他 ddd4j-ai 模块。

## 5. 接口规格

### 5.1 AiComponent

```java
public interface AiComponent {
    /**
     * Stable component name used for routing, diagnostics, and configuration.
     * @return component name
     */
    String name();
}
```

- 用途：路由定位、日志诊断、配置按名绑定。
- 实现约定：返回值应为稳定常量（不随实例变化），建议在实现类中以常量返回。

### 5.2 AiHandler

```java
public interface AiHandler extends AiComponent {
    /**
     * Handles a request and returns a normalized response.
     * @param request request
     * @return response
     */
    AiResponse handle(AiRequest request);
}
```

- 同步调用契约；异步/流式由各组件扩展，不在此层强行规定。
- 继承 `AiComponent`，故每个 handler 自带 `name()`。

### 5.3 AiRequest

```java
public record AiRequest(String input, Map<String, Object> metadata) {
    public AiRequest {
        input = Objects.requireNonNull(input, "input");
        metadata = Objects.isNull(metadata) ? Map.of() : Map.copyOf(metadata);
    }
    public static AiRequest of(String input);
}
```

- `input`：用户输入或归一化 prompt，必填。
- `metadata`：调用方提供的元数据；null 退化为空映射，非空则防御性拷贝（调用方后续修改不影响实例）。

### 5.4 AiResponse

```java
public record AiResponse(String output, Map<String, Object> metadata) {
    public AiResponse {
        output = Objects.requireNonNull(output, "output");
        metadata = Objects.isNull(metadata) ? Map.of() : Map.copyOf(metadata);
    }
    public static AiResponse of(String output);
}
```

- `output`：模型或组件输出，必填（失败语义由 metadata 或上层封装表达，非 null）。
- `metadata`：响应元数据（如 token 用量、模型名、延迟等），同 AiRequest 不可变策略。

## 6. 模块落点

- 模块：`ddd4j-ai-core`
- 包：`io.ddd4j.ai.core`
- pom：纯 Java，不声明 Spring 依赖；版本由 `ddd4j-ai-bom` 治理。

## 7. 测试策略

> 现状：core 当前零测试。补齐计划见 `2026-08-07-ddd4j-ai-v1-core-and-sst.md`。

- **record 契约测试**：
  - `AiRequest.of(x)` 等价于 `new AiRequest(x, Map.of())`。
  - `input` 为 null 抛 `NullPointerException`。
  - `metadata` 为 null 时 `metadata()` 返回空映射；传入可变 Map 后修改原 Map 不影响实例（防御性拷贝）。
  - `AiResponse` 同上对称校验。
- **AiHandler 行为测试**：以测试桩 `AiHandler` 实现验证 `handle(AiRequest)` → `AiResponse` 往返，并校验 `name()` 稳定返回。

## 8. 演进备注

- 流式响应：未来如需统一流式协议，建议在 core 追加 `AiStreamHandler` 或返回 `Flow`/`Publisher` 类型，不破坏现有同步契约。
- 强类型 metadata：若诊断字段收敛，可引入可选的强类型 metadata record，保持 `Map` 重载向后兼容。
