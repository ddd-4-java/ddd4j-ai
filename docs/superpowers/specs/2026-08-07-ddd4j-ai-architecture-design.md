# Ddd4j AI 整体架构与组件契约设计

- 日期：2026-08-07
- 作者：PartMe.AI
- 状态：设计已确认（core 契约与 BOM 治理已落地，模块命名规范于 `3fbd757` 定型）
- 范围：ddd4j-ai 全项目 —— 架构理念、Maven 模块拓扑、组件契约（core 五类）、COLA 分层约定、依赖与版本治理、组件状态总表
- 关联文档：各组件设计文档见 `docs/superpowers/specs/` 同目录；实施路线见 `docs/superpowers/plans/`

---

## 1. 背景

**Ddd4j AI** 是 [Ddd4j Boot](../../../README.md) 生态下的 **AI 能力扩展项目**，面向基于领域驱动设计（DDD）与 COLA 架构的业务系统，提供可插拔的 AI 组件模块。

项目继承 `spring-boot-starter-parent` 构建规范，通过 `ddd4j-ai-dependencies` → `ddd4j-boot-dependencies` 链式治理第三方版本，在 [Spring AI](https://docs.spring.io/spring-ai/reference/index.html) 2.0.0 基础上，统一管理 Spring AI Alibaba、MCP、向量检索、语音处理等 AI 依赖版本，并以 **组件化（cmpt）** 方式向业务服务暴露能力。

本设计文档从原 README 迁入，作为所有组件级 spec 的公共引用基线：组件 spec 的"架构"章节均以本文档第 4、6、7、8 节为准。

## 2. 目标

- **降低 AI 集成成本**：统一 BOM 与组件封装，避免各业务服务重复对接 Spring AI 与供应商 SDK。
- **保持架构一致性**：AI 能力以基础设施组件形式接入，不侵入领域模型；对外部大模型、向量库、语音服务通过端口接口与防腐层（ACL）隔离供应商 API 变化。
- **支持渐进式演进**：每个 AI 能力独立为 Maven 子模块，业务服务按需引入；组件模块可独立开发与发布。
- **覆盖典型 LLM 应用场景**：对话、记忆、嵌入、向量库、RAG、Agent 编排、工具调用（MCP）、工作流、语音与文档处理、智能路由。

### 非目标

- **不实现领域业务规则**：业务规则仍保留在业务服务的领域层，AI 组件仅提供应用层可调用的服务能力。
- **不绑定单一供应商**：通过端口接口与 SPI 思路允许多后端切换（如 Azure Speech / WhisperCpp / Edge TTS）。
- **不替代 Spring AI**：组件在 Spring AI 抽象之上做 COLA 分层封装，而非重新实现模型客户端。

## 3. 关键决策

| # | 决策 | 理由 |
|---|------|------|
| D1 | core 契约为**纯 Java**，不依赖 Spring | 契约层必须可在任意宿主（Spring/非 Spring）复用，保证稳定性 |
| D2 | `AiHandler extends AiComponent` 而非平行接口 | 所有可调用处理器同时具备稳定命名（`name()`），路由与诊断无需额外接口 |
| D3 | `AiRequest` / `AiResponse` 使用 **record + 紧凑构造器不可变校验** | 请求/响应天然不可变；`metadata` 缺省 `Map.of()`，非空时 `Map.copyOf` 防御性拷贝 |
| D4 | 组件以 **COLA 菱形架构** 落点为 Infrastructure 适配器 | AI 组件是 COLA 中的基础设施层，通过端口接口（`service` 包）与领域/应用层解耦 |
| D5 | BOM **链式 import**：`ddd4j-ai-bom`（组件版本）+ `ddd4j-ai-dependencies`（第三方版本，import boot-dependencies） | 版本治理分层，业务服务继承 `ddd4j-ai-parent` 即获全部版本对齐 |
| D6 | 子包约定 `cmpt`（component） | 与 Boot 组件范式对齐，AI 能力统一在 `io.ddd4j.ai.extension.<domain>` 下 |

## 4. 总体架构

### 4.1 与 Ddd4j Boot 协同

- **统一父工程**：根工程继承 `spring-boot-starter-parent`；`ddd4j-ai-dependencies` import `ddd4j-boot-dependencies`；业务服务继承 `ddd4j-ai-parent`（import `ddd4j-ai-bom`）。
- **组件化交付**：每个 AI 能力独立为 Maven 子模块，业务服务按需引入，避免单体 AI 依赖膨胀。
- **基础设施层定位**：AI 组件作为 COLA 架构中的 **Infrastructure 适配器**，通过端口接口与领域层解耦。

### 4.2 领域驱动设计（DDD）

- **限界上下文**：对话、RAG、Agent、语音等能力按子域拆分为独立组件，便于按业务边界演进。
- **防腐层（ACL）**：对外部大模型、向量库、语音服务等通过组件封装，隔离供应商 API 变化。
- **充血模型友好**：组件提供应用层可调用的服务能力，业务规则仍保留在领域层。

### 4.3 架构模式

- **菱形架构（COLA）**：组件模块遵循 `interfaces → application → domain ← infrastructure` 依赖方向（随模块演进逐步完善）。
- **依赖倒置**：领域网关接口由业务服务定义，AI 组件在基础设施层提供实现。
- **BOM 版本对齐**：`ddd4j-ai-bom` 管理 AI 组件模块版本；`ddd4j-ai-dependencies` 管理 AI 第三方依赖并 import Boot 依赖 BOM。

## 5. Maven 模块架构

**模块表**：

| 模块 | 说明 |
|------|------|
| ddd4j-ai-bom | AI 扩展 BOM：统一管理各 `ddd4j-ai-extension-*` 模块版本，外部项目 import 引用 |
| ddd4j-ai-dependencies | AI 依赖 BOM：import `ddd4j-boot-dependencies`，并追加 Spring AI / MCP / 语音等版本 |
| ddd4j-ai-extensions | 扩展聚合模块（parent 为 `ddd4j-ai-dependencies`，对齐 `ddd4j-boot-mq` 范式） |
| ddd4j-ai-parent | 业务 AI 服务 Parent：parent 为 `ddd4j-ai-dependencies`，import `ddd4j-ai-bom` |
| ddd4j-ai-samples | 示例模块集合，展示各 AI 组件在业务服务中的集成方式（持续完善中） |

**模块结构树**：

```
|--ddd4j-ai
|----ddd4j-ai-bom                           # BOM 依赖管理
|----ddd4j-ai-dependencies                 # 第三方 AI 依赖版本控制
|----ddd4j-ai-core                         # AI 通用纯 Java 契约（已实现）
|----ddd4j-ai-extensions                   # AI 扩展父模块
|------ddd4j-ai-extension-chat             # 对话扩展
|------ddd4j-ai-extension-memory           # 记忆体扩展
|------ddd4j-ai-extension-embedding        # 向量嵌入扩展
|------ddd4j-ai-extension-vectordb         # 向量数据库扩展
|------ddd4j-ai-extension-agent            # 智能体扩展
|------ddd4j-ai-extension-flow             # 工作流扩展
|------ddd4j-ai-extension-rag              # 检索增强（RAG）扩展
|------ddd4j-ai-extension-asr              # 语音识别（ASR）扩展
|------ddd4j-ai-extension-tts              # 文本转语音（TTS）扩展
|------ddd4j-ai-extension-sst              # 语音合成/识别一体化扩展（已实现 Azure Speech）
|------ddd4j-ai-extension-mcp              # 模型上下文协议（MCP）扩展
|------ddd4j-ai-extension-ocr              # 光学字符识别（OCR）扩展
|------ddd4j-ai-extension-router           # 智能路由扩展
|----ddd4j-ai-parent                       # 业务 AI 服务父 POM
|----ddd4j-ai-samples                      # 示例工程（规划中）
```

## 6. 组件模块状态总表

> 状态以实际代码为准（核对于 2026-08-16）。

| 组件模块 | 功能 | 当前状态 | 真实代码 |
|----------|------|----------|----------|
| ddd4j-ai-core | AI 通用纯 Java 契约，不绑定 Spring Boot | **已实现** | 5 类（见第 7 节） |
| ddd4j-ai-extension-chat | AI 对话能力，基于 Spring AI ChatClient 封装 | **已实现**（v1.x-A） | 端口+适配器+AutoConfig |
| ddd4j-ai-extension-memory | 会话记忆与上下文管理 | **已实现**（v1.x-A） | 窗口实现+可插拔 repository |
| ddd4j-ai-extension-embedding | 文本向量嵌入（Embedding） | **已实现**（v1.x-A） | 批量分片适配器 |
| ddd4j-ai-extension-vectordb | 向量数据库接入与检索 | **已实现**（v1.x-A） | 通用 VectorStore 适配器 |
| ddd4j-ai-extension-agent | AI 智能体编排与工具调用 | 骨架 | 无 |
| ddd4j-ai-extension-flow | AI 工作流（对接 Spring AI Alibaba Graph 等） | 骨架 | 无 |
| ddd4j-ai-extension-rag | 检索增强生成（RAG）管道 | **已实现**（v1.x-B 提前交付） | 管道+Reranker 扩展点 |
| ddd4j-ai-extension-asr | 自动语音识别（ASR），规划对接 WhisperCpp 等 | 骨架 | 无 |
| ddd4j-ai-extension-tts | 文本转语音（TTS），规划对接 Edge TTS 等 | 骨架 | 无 |
| ddd4j-ai-extension-sst | 语音合成与识别，已实现 Azure Speech + FFmpeg 音频转换 | **已实现** | 12 类 |
<<<<<<< HEAD
| ddd4j-ai-extension-mcp | Model Context Protocol 工具与资源接入 | 骨架 | 无 |
| ddd4j-ai-extension-ocr | 文档/图像 OCR，规划对接 PDFBox、Tika 等 | 骨架 | 无 |
| ddd4j-ai-extension-router | 多模型智能路由与负载策略 | 骨架 | 无 |
| ddd4j-ai-samples | 示例工程 | **已实现**（sst/chat/memory/embedding/vectordb/rag 六示例） | 6 示例类 |
=======
| ddd4j-ai-extension-mcp | Model Context Protocol 工具与资源接入 | **已实现**（2026-08-25） | 端口+Spring AI 客户端适配 |
| ddd4j-ai-extension-ocr | 文档/图像 OCR，对接 PDFBox、Tika | **已实现**（2026-08-25） | PDFBox 直提+Tika 多格式 |
| ddd4j-ai-extension-router | 多模型智能路由与负载策略 | **已实现**（2026-08-25） | 轮询/权重/延迟策略 |
| ddd4j-ai-extension-document | 智能体文档统一读取门面（Tika 底座，markitdown 全能力对齐） | **已实现 + 生产加固**（2026-08-27） | 结构化 Markdown/章节树/表格/图片/OCR/音频转写/嵌入资源；流式解析+限额+超时+SecureContentHandler+SLF4J 可观测 |
| ddd4j-ai-samples | 示例工程 | **已实现**（13 组件示例 + 7 集成场景） | 20 示例类（含 RAG 全链路/完整智能体/多智能体协作等） |
>>>>>>> 93f856e (feat(samples): add 7 integrated scenarios (chat-memory/rag-pipeline/smart-agent/knowledge-base/multi-model-router/doc-understanding/orchestration) + update architecture spec)

> 已实现组件统一遵循 COLA 分层（service 端口 + impl + properties）与标准 Spring Boot `@AutoConfiguration`（`AutoConfiguration.imports`）；sst 为历史实现（组件扫描装配），保持存量不动。实现将随版本迭代逐步补齐，演进路线见 `docs/superpowers/plans/`。

## 7. Core 契约规格（ddd4j-ai-core）

包 `io.ddd4j.ai.core`，纯 Java（pom 不引 Spring），是所有扩展组件的公共稳定契约。

```java
// 稳定命名契约：路由、诊断、配置定位
public interface AiComponent {
    String name();
}

// 最小调用契约：处理器同时是可命名组件
public interface AiHandler extends AiComponent {
    AiResponse handle(AiRequest request);
}

// 不可变请求：input 必填，metadata 缺省空映射、非空时防御性拷贝
public record AiRequest(String input, Map<String, Object> metadata) {
    public AiRequest { /* Objects.requireNonNull(input); metadata 不可变拷贝 */ }
    public static AiRequest of(String input);   // 便捷工厂，metadata=Map.of()
}

// 不可变响应：output 必填，metadata 同上
public record AiResponse(String output, Map<String, Object> metadata) {
    public AiResponse { /* 同上 */ }
    public static AiResponse of(String output);
}
```

> 各契约类的详细设计决策（为何 record、为何继承、metadata 选型）见 `2026-07-01-ai-core-contract-design.md`。

## 8. COLA 分层约定（组件模块目录结构）

AI 扩展遵循与 Ddd4j Boot 一致的 COLA 分层约定，以 `ddd4j-ai-extension-sst` 为参考形态：

```
ddd4j-ai-extension-<domain>/
├── src/main/java/io/ddd4j/ai/cmpt/<domain>
│   ├── dto/                   # 数据传输对象（Adapter 入参）
│   ├── vo/                    # 视图对象（Adapter 出参）
│   ├── enums/                 # 枚举与常量
│   ├── properties/            # 配置属性（Infrastructure）
│   ├── service/               # 服务能力（端口接口）
│   │   └── impl/              # 基础设施适配器实现
│   ├── application/           # 应用服务（随模块演进补齐）
│   ├── domain/                # 领域模型（随模块演进补齐）
│   └── interfaces/            # 接口适配（随模块演进补齐）
└── src/main/resources/
```

**依赖方向**：`service（端口）` ← `impl（基础设施适配器）`，业务应用层通过 Spring 注入端口接口调用，不直接依赖供应商 SDK。其余组件（chat、rag、agent 等）按相同分层约定逐步填充 `application`、`domain`、`infrastructure`、`interfaces` 包结构。

## 9. 依赖与版本治理（BOM 链）

### 9.1 BOM 链路

```
业务服务
  └─ inherits ddd4j-ai-parent
       └─ parent = ddd4j-ai-dependencies  (import ddd4j-boot-dependencies)
            └─ import ddd4j-ai-bom        (管理各 extension-* 模块版本)
```

业务服务通过继承 `ddd4j-ai-dependencies` 的 `dependencyManagement` 即可获得第三方版本对齐，无需在各模块重复声明。

### 9.2 AI 第三方依赖版本（ddd4j-ai-dependencies 实际声明）

> 以 `ddd4j-ai-dependencies/pom.xml` 为事实源（核对于 2026-08-16，当日已升级至 Maven Central 最新稳定版）。

| 依赖 | 版本 | 属性 |
|------|------|------|
| Spring AI BOM | 2.0.0 | `spring-ai.version` |
| Spring AI Alibaba BOM | 2.0.0-M1.1 | `spring-ai-alibaba.version` |
| Spring AI Alibaba Extensions BOM | 2.0.0-M1.1 | 同上 |
| Spring AI Alibaba Graph Core | 2.0.0-M1.1 | `spring-ai-alibaba-graph.version` |
| Spring AI Community（Moonshot/千帆） | 1.0.0 | `spring-ai-community.version` |
| LangChain4j BOM | 1.19.0 | `langchain4j.version` |
| AgentScope Java BOM | 2.0.2 | `agentscope-java.version` |
| MCP SDK BOM | 2.0.0 | `mcp-bom.version` |
| Microsoft Speech SDK | 1.51.1 | `microsoft-speech-sdk.version` |
| WhisperCpp | 1.4.0 | `whispercpp.version` |
| tts-edge-java | 1.3.3 | `tts-edge-java.version` |
| Protobuf Java | 4.35.1 | `protobuf-java.version` |
| proto-google-common-protos | 2.74.0 | `google-protos.version` |
| ddd4j-boot-dependencies | 3.4.x.20260630-SNAPSHOT | `ddd4j-boot.version` |

> 版本升级记录（2026-08-16）：langchain4j 1.18.1→1.19.0、agentscope 2.0.1→2.0.2、speech-sdk 1.47.0→1.51.1、protobuf 4.31.0→4.35.1、proto-google-common-protos 2.57.0→2.74.0、tts-edge-java 1.3.1→1.3.3。升级后全模块 38 个单元测试全绿。
> 备注：① protobuf 的 Central latest 指向 4.36.0-RC2，已排除 RC 选用最新稳定版 4.35.1；② agentscope Central latest 指向 2.0.2-subagent-bugfix（补丁变体），选用正式版 2.0.2；③ `jsonschema.version=5.0.0` 属性当前未被 dependencyManagement 引用（历史残留）。

### 9.3 待确认依赖（README 历史列出但 pom 未直接声明）

| 依赖 | README 标注 | pom 实际 | 处置 |
|------|------------|---------|------|
| Apache PDFBox | 3.0.5 | **未在 ai-dependencies 声明** | 待确认是否经 `ddd4j-boot-dependencies` 间接提供；OCR 模块实施前需补齐或确认 |
| Apache Tika BOM | 3.2.2 | **未在 ai-dependencies 声明** | 同上 |

### 9.4 已知兼容性约束：Spring AI 2.0 与父链 Spring Framework 6.2（2026-08-16 发现）

Spring AI 2.0.0 的运行时（`OllamaApi` 等模型客户端）依赖 Spring Framework 7 新增的
`org.springframework.core.retry` 包；当前父链 `ddd4j-boot-dependencies`（3.4.x）锁定
Spring Framework 6.2，不含该包。影响与对策：

- **组件层不受影响**：本项目的端口/适配器/AutoConfiguration 仅使用两代稳定兼容的 API（ChatClient、EmbeddingModel、VectorStore、@ConfigurationProperties），在 FW 6.2 下编译与装配测试全部通过。
- **真实模型链路受限**：容器内真实模型调用（Ollama 测试）在 FW 6.2 类路径下会抛 `NoClassDefFoundError`，相关集成测试已按 classpath 探测自动跳过并注明原因。
- **建议决策**：业务侧引入模型 starter 前，需将运行时升级到 Spring Boot 4 / FW 7，或由 ddd4j-boot-dependencies 提供 spring-ai 2.0 兼容线；单纯提升 spring-core 至 7（与 spring-context 6.2 混搭）不可行（`MimeType` 内部类已被 FW7 移除，实测编译失败）。

## 10. 测试策略

- **现状**：全项目零单元测试（所有 `src/test/` 为空目录占位）。
- **目标**：组件契约层（端口接口）以契约测试覆盖；基础设施适配器（impl）对供应商 SDK 做 mock 测试；properties 绑定做 `@ConfigurationProperties` 校验测试。
- **补齐计划**：见 `2026-08-07-ddd4j-ai-v1-core-and-sst.md` 中为 core / sst 补写单元测试的待办 Task。

## 11. 模块落点与发布

- 组件模块版本由 `ddd4j-ai-bom` 统一管理，`${ddd4j-ai.version}` 占位。
- 项目版本：`1.0.0-SNAPSHOT`（根 pom `<revision>1.0.0-SNAPSHOT</revision>`，Maven flatten 范式）。
- 环境要求：JDK 17+（与 ddd4j-boot 一致）、Maven 3.8+；使用语音组件时需安装 [FFmpeg](https://ffmpeg.org/) 并确保 `ffmpeg` 命令在 PATH 中可用。
- 本地构建：`./mvnw clean install -DskipTests`。
