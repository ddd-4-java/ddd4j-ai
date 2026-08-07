## Ddd4j AI 简介

**Ddd4j AI** 是 [Ddd4j Boot](../ddd4j-boot/README.md) 生态下的 **AI 能力扩展项目**，面向基于领域驱动设计（DDD）与 COLA 架构的业务系统，提供可插拔的 AI 组件模块。

项目继承 `spring-boot-starter-parent` 构建规范，通过 `ddd4j-ai-dependencies` → `ddd4j-boot-dependencies` 链式治理第三方版本，在 [Spring AI](https://docs.spring.io/spring-ai/reference/index.html) 1.0.x 基础上，统一管理 Spring AI Alibaba、MCP、向量检索、语音处理等 AI 依赖版本，并以 **组件化（cmpt）** 方式向业务服务暴露能力。

### 🎯 核心设计理念

#### **与 Ddd4j Boot 协同**

- **统一父工程**：根工程继承 `spring-boot-starter-parent`；`ddd4j-ai-dependencies` import `ddd4j-boot-dependencies`；业务服务继承 `ddd4j-ai-parent`（import `ddd4j-ai-bom`）
- **组件化交付**：每个 AI 能力独立为 Maven 子模块，业务服务按需引入，避免单体 AI 依赖膨胀
- **基础设施层定位**：AI 组件作为 COLA 架构中的 **Infrastructure 适配器**，通过端口接口与领域层解耦

#### **领域驱动设计（DDD）**

- **限界上下文**：对话、RAG、Agent、语音等能力按子域拆分为独立组件，便于按业务边界演进
- **防腐层（ACL）**：对外部大模型、向量库、语音服务等通过组件封装，隔离供应商 API 变化
- **充血模型友好**：组件提供应用层可调用的服务能力，业务规则仍保留在领域层

#### **架构模式**

- **菱形架构（COLA）**：组件模块遵循 `interfaces → application → domain ← infrastructure` 依赖方向（随模块演进逐步完善）
- **依赖倒置**：领域网关接口由业务服务定义，AI 组件在基础设施层提供实现
- **BOM 版本对齐**：`ddd4j-ai-bom` 管理 AI 组件模块版本；`ddd4j-ai-dependencies` 管理 AI 第三方依赖并 import Boot 依赖 BOM

### ✨ 主要特性

- **1. 模块化 AI 组件**：覆盖对话（Chat）、记忆（Memory）、嵌入（Embedding）、向量库（VectorDB）、智能体（Agent）、工作流（Flow）、RAG、ASR/TTS/SST、MCP、OCR、智能路由等能力域

- **2. Spring AI 生态集成**：统一管理 Spring AI BOM（1.0.3）、Spring AI Alibaba、Spring AI Community（Moonshot、千帆等）及扩展包版本

- **3. MCP 协议支持**：通过 `mcp-bom` 管理 Model Context Protocol SDK，为 Agent 工具调用提供标准化接入

- **4. 多模态能力规划**：依赖管理中已纳入 WhisperCpp、Edge TTS、Microsoft Speech SDK、PDFBox、Apache Tika 等，支撑语音与文档处理场景

- **5. 与 Ddd4j Boot 无缝集成**：业务服务继承 `ddd4j-ai-parent` 或引入 `ddd4j-ai-bom`，即可在现有 DDD 分层项目中叠加 AI 能力

- **6. 语音组件已落地**：`ddd4j-ai-extension-sst` 已实现基于 Azure Cognitive Speech 的 TTS/STT 及 FFmpeg 音频格式转换

### 📦 项目定位

**Ddd4j AI** 面向需要在 DDD/COLA 业务系统中集成大模型能力的团队，旨在：

- **降低 AI 集成成本**：统一 BOM 与组件封装，避免各业务服务重复对接 Spring AI 与供应商 SDK
- **保持架构一致性**：AI 能力以基础设施组件形式接入，不侵入领域模型
- **支持渐进式演进**：组件模块可独立开发与发布，业务按需引入
- **覆盖典型 LLM 应用场景**：对话、RAG、Agent 编排、工具调用（MCP）、语音与文档处理

### 🏗️ 项目架构

**Maven 模块架构**：

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
|----ddd4j-ai-core                         # AI 通用纯 Java 契约
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

**组件模块说明**：

| 组件模块 | 说明 | 当前状态 |
|----------|------|----------|
| ddd4j-ai-core | AI 通用纯 Java 契约，不绑定 Spring Boot | 骨架模块 |
| ddd4j-ai-extension-chat | AI 对话能力，基于 Spring AI ChatClient 封装 | 骨架模块 |
| ddd4j-ai-extension-memory | 会话记忆与上下文管理 | 骨架模块 |
| ddd4j-ai-extension-embedding | 文本向量嵌入（Embedding） | 骨架模块 |
| ddd4j-ai-extension-vectordb | 向量数据库接入与检索 | 骨架模块 |
| ddd4j-ai-extension-agent | AI 智能体编排与工具调用 | 骨架模块 |
| ddd4j-ai-extension-flow | AI 工作流（对接 Spring AI Alibaba Graph 等） | 骨架模块 |
| ddd4j-ai-extension-rag | 检索增强生成（RAG）管道 | 骨架模块 |
| ddd4j-ai-extension-asr | 自动语音识别（ASR），规划对接 WhisperCpp 等 | 骨架模块 |
| ddd4j-ai-extension-tts | 文本转语音（TTS），规划对接 Edge TTS 等 | 骨架模块 |
| ddd4j-ai-extension-sst | 语音合成与识别，已实现 Azure Speech + FFmpeg 音频转换 | **已实现** |
| ddd4j-ai-extension-mcp | Model Context Protocol 工具与资源接入 | 骨架模块 |
| ddd4j-ai-extension-ocr | 文档/图像 OCR，规划对接 PDFBox、Tika 等 | 骨架模块 |
| ddd4j-ai-extension-router | 多模型智能路由与负载策略 | 骨架模块 |

> **说明**：除 `ddd4j-ai-extension-sst` 外，其余扩展当前为模块骨架（包结构与 POM 已就绪），实现将随版本迭代逐步补齐。

### 📖 使用说明

#### 1. 外部项目引用（推荐使用 BOM）

在外部项目的 `pom.xml` 中引入 BOM：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.ddd4j.ai</groupId>
            <artifactId>ddd4j-ai-bom</artifactId>
            <version>${ddd4j-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

然后按需引入组件，无需指定版本：

```xml
<dependencies>
    <dependency>
        <groupId>io.ddd4j.ai</groupId>
        <artifactId>ddd4j-ai-extension-sst</artifactId>
    </dependency>
</dependencies>
```

> 引入 BOM 后按需声明组件，无需指定版本。

#### 2. 内部 / 业务服务使用

业务 AI 服务继承 `ddd4j-ai-parent`（已 import `ddd4j-ai-bom`，第三方版本由 `ddd4j-ai-dependencies` 链式提供）：

```xml
<parent>
    <groupId>io.ddd4j.ai</groupId>
    <artifactId>ddd4j-ai-parent</artifactId>
    <version>${revision}</version>
    <relativePath>../ddd4j-ai-parent/pom.xml</relativePath>
</parent>
```

若同时需要 Ddd4j Boot 基础能力，可额外 import `ddd4j-boot-bom` 与对应 `ddd4j-boot-*` 组件（Boot 版本由 `ddd4j-ai-dependencies` 已 import 的 `ddd4j-boot-dependencies` 对齐）。

#### 3. 本地构建

```bash
# 在项目根目录执行
./mvnw clean install -DskipTests
```

**环境要求**：

- JDK 17+（与 ddd4j-boot 一致）
- Maven 3.8+
- 使用语音组件时需安装 [FFmpeg](https://ffmpeg.org/) 并确保 `ffmpeg` 命令在 PATH 中可用

### ⚙️ 配置说明

#### Azure Speech（ddd4j-ai-extension-sst）

在 `application.yml` 中配置 Azure 认知语音服务：

```yaml
azure:
  speech:
    key: ${AZURE_SPEECH_KEY}           # Azure Speech 订阅密钥
    region: eastasia                   # 服务区域
    voice-name: zh-CN-XiaoxiaoNeural   # TTS 音色
    recognition-language: zh-CN        # STT 识别语言
```

配置类：`io.ddd4j.ai.cmpt.sst.properties.AzureSpeechProperties`（前缀 `azure.speech`）。

#### Spring AI 相关依赖版本（ddd4j-ai-dependencies 统一管理）

| 依赖 | 版本 |
|------|------|
| Spring AI BOM | 2.0.0 |
| Spring AI Alibaba BOM | 2.0.0-M1.1 |
| Spring AI Alibaba Extensions BOM | 2.0.0-M1.1 |
| LangChain4j BOM | 1.18.1 |
| AgentScope Java BOM | 2.0.1 |
| Spring AI Community | 1.0.0 |
| MCP SDK BOM | 2.0.0 |
| Microsoft Speech SDK | 1.47.0 |
| WhisperCpp | 1.4.0 |
| tts-edge-java | 1.3.1 |
| Apache PDFBox | 3.0.5 |
| Apache Tika BOM | 3.2.2 |

业务服务通过继承 `ddd4j-ai-dependencies` 的 `dependencyManagement` 即可获得上述版本对齐，无需在各模块重复声明。

### 🚀 快速开始

以下示例展示如何在 Spring Boot 业务服务中使用已实现的 **语音组件（sst）**。

#### 1. 添加依赖

```xml
<dependency>
    <groupId>io.ddd4j.ai</groupId>
    <artifactId>ddd4j-ai-extension-sst</artifactId>
</dependency>
```

#### 2. 配置 Azure Speech

见上文 [Azure Speech 配置](#azure-speechddd4j-ai-extension-sst)。

#### 3. 注入并使用 SpeechService

核心接口：`io.ddd4j.ai.cmpt.sst.service.SpeechService`

```java
@Autowired
private SpeechService<SpeechConfig> speechService;

// 文本转语音（同步，返回 MP3 字节流）
TTSResultVO result = speechService.tts(null, "你好，欢迎使用 Ddd4j AI");

// 文本转语音（回调方式）
speechService.text2Voice("你好", audioBytes -> {
    // 处理合成后的音频数据
});

// 语音转文本（WAV 字节流）
speechService.voice2TextFromWavByteArray(null, wavBytes, text -> {
    // 识别成功，text 为识别结果
});

// 语音转文本（MP3 字节流）
speechService.voice2TextFromMp3ByteArray(null, mp3Bytes, text -> {
    // 处理识别结果
});
```

#### 4. 音频格式转换（FFmpeg）

`FFmpegService` 提供常见格式互转，供 STT 前置处理使用：

```java
@Autowired
private FFmpegService ffmpegService;

// 任意音频字节流 → 16kHz 单声道 WAV（Azure STT 推荐格式）
byte[] wavBytes = ffmpegService.convertAudioToWavFromByteAry(sourceBytes);

// WAV → MP3
byte[] mp3Bytes = ffmpegService.convertWavToMp3FromByteAry(wavBytes);
```

#### 5. 请求/响应模型

| 类型 | 类名 | 说明 |
|------|------|------|
| 请求 DTO | `STTDto` | 语音转文本入参（channel、formatType、data） |
| 请求 DTO | `TTSDto` | 文本转语音入参（text） |
| 响应 VO | `STTResultVO` | 识别结果（status、msg、text、reason） |
| 响应 VO | `TTSResultVO` | 合成结果（status、msg、audio、reason） |
| 渠道枚举 | `TTSSTTChannel` | 当前支持 `Azure` |
| 音频格式 | `AVFormatEnums` | 音频采样率与编码格式枚举 |

### 📁 组件模块目录结构

AI 扩展遵循与 Ddd4j Boot 一致的 COLA 分层约定，以 `ddd4j-ai-extension-sst` 为例：

```
ddd4j-ai-extension-sst/
├── src/main/java/io/ddd4j/ai/cmpt/sst
│   ├── dto/                   # 数据传输对象（Adapter 入参）
│   │   ├── STTDto.java
│   │   └── TTSDto.java
│   ├── vo/                    # 视图对象（Adapter 出参）
│   │   ├── STTResultVO.java
│   │   └── TTSResultVO.java
│   ├── enums/                 # 枚举与常量
│   │   ├── AVFormatEnums.java
│   │   ├── TTSSTTChannel.java
│   │   └── ConvertKeyEnums.java
│   ├── properties/            # 配置属性（Infrastructure）
│   │   └── AzureSpeechProperties.java
│   └── service/               # 服务能力
│       ├── SpeechService.java              # 端口接口
│       ├── SpeechServiceText2VoiceCallback.java
│       ├── SpeechServiceVoice2TextCallback.java
│       └── impl/
│           ├── AzureSpeechService.java     # Azure 适配器实现
│           └── FFmpegService.java          # FFmpeg 音频转换
└── src/main/resources/
```

**依赖方向**：`service（端口）` ← `impl（基础设施适配器）`，业务应用层通过 Spring 注入 `SpeechService` 调用，不直接依赖 Azure SDK。

其余组件（chat、rag、agent 等）将按相同分层约定逐步填充 `application`、`domain`、`infrastructure`、`interfaces` 包结构。

### 🔗 相关资源

- [Ddd4j Boot 项目](../ddd4j-boot/README.md)
- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/index.html)
- [Spring AI Alibaba](https://github.com/alibaba/spring-ai-alibaba)
- [Model Context Protocol](https://modelcontextprotocol.io/)
- [Azure Cognitive Speech SDK](https://learn.microsoft.com/azure/ai-services/speech-service/)

### 📄 许可证

本项目采用与 Ddd4j Boot 一致的许可证，详见 [LICENSE](./LICENSE)。
