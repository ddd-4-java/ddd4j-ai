## Ddd4j AI 简介

**Ddd4j AI** 是 [Ddd4j Boot](../ddd4j-boot/README.md) 生态下的 **AI 能力扩展项目**，面向基于领域驱动设计（DDD）与 COLA 架构的业务系统，提供可插拔的 AI 组件模块。

项目继承 `spring-boot-starter-parent` 构建规范，通过 `ddd4j-ai-dependencies` → `ddd4j-boot-dependencies` 链式治理第三方版本，在 [Spring AI](https://docs.spring.io/spring-ai/reference/index.html) 2.0.0 基础上，统一管理 Spring AI Alibaba、MCP、向量检索、语音处理等 AI 依赖版本，并以 **组件化（cmpt）** 方式向业务服务暴露能力。

### ✨ 主要特性

- **1. 模块化 AI 组件**：覆盖对话（Chat）、记忆（Memory）、嵌入（Embedding）、向量库（VectorDB）、智能体（Agent）、工作流（Flow）、RAG、ASR/TTS/SST、MCP、OCR、智能路由等能力域
- **2. Spring AI 生态集成**：统一管理 Spring AI BOM（2.0.0）、Spring AI Alibaba、Spring AI Community（Moonshot、千帆等）及扩展包版本
- **3. MCP 协议支持**：通过 `mcp-bom` 管理 Model Context Protocol SDK，为 Agent 工具调用提供标准化接入
- **4. 多模态能力**：依赖管理中已纳入 WhisperCpp、Edge TTS、Microsoft Speech SDK、PDFBox、Apache Tika 等，支撑语音与文档识别场景
- **5. 与 Ddd4j Boot 无缝集成**：业务服务继承 `ddd4j-ai-parent` 或引入 `ddd4j-ai-bom`，即可在现有 DDD 分层项目中叠加 AI 能力
- **6. 组件全部落地**：`ddd4j-ai-core` 通用 AI 契约；sst（Azure Speech TTS/STT）、chat/memory/embedding/vectordb/rag、agent（ReAct/Plan-Execute）、flow（Graph 编排）、router（多模型路由）、mcp（工具调用）、ocr（PDFBox+Tika）、asr（WhisperCpp）、tts（Edge TTS）、document（Tika 底座统一文档读取：markitdown 全能力对齐 + 生产加固——流式解析/限额超时/zip 炸弹防护/可观测）

### 📦 项目定位

**Ddd4j AI** 面向需要在 DDD/COLA 业务系统中集成大模型能力的团队，旨在：

- **降低 AI 集成成本**：统一 BOM 与组件封装，避免各业务服务重复对接 Spring AI 与供应商 SDK
- **保持架构一致性**：AI 能力以基础设施组件形式接入，不侵入领域模型
- **支持渐进式演进**：组件模块可独立开发与发布，业务按需引入
- **覆盖典型 LLM 应用场景**：对话、RAG、Agent 编排、工具调用（MCP）、语音与文档处理

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

配置类：`io.ddd4j.ai.extension.sst.properties.AzureSpeechProperties`（前缀 `azure.speech`）。

> 第三方依赖版本（Spring AI / Alibaba / MCP / LangChain4j / AgentScope / 语音 SDK 等）统一由 `ddd4j-ai-dependencies` 管理，详见架构设计文档第 9 节。

### 🚀 快速开始

#### 0. 一键体验：可运行示例应用（推荐）

[`ddd4j-ai-sample-app`](./ddd4j-ai-sample-app/) 是开箱即用的演示工程（Ollama + chat/memory/embedding/vectordb/rag 全组件 + REST 端点）：

```bash
# 1. 准备 Ollama 与模型
ollama pull qwen2.5:0.5b && ollama pull all-minilm

# 2. 启动示例应用（默认连接 http://localhost:11434）
./mvnw -pl ddd4j-ai-sample-app spring-boot:run

# 3. 体验端点
curl "http://localhost:8080/chat?q=你好"
curl -X POST "http://localhost:8080/chat/session?q=我叫小明&conversationId=demo"
curl -X POST "http://localhost:8080/rag/ingest?content=灵犀计划是...内部代号"
curl "http://localhost:8080/rag/ask?q=灵犀计划是什么"
curl -N "http://localhost:8080/chat/stream?q=讲个笑话"   # SSE 流式
```

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

核心接口：`io.ddd4j.ai.extension.sst.service.SpeechService`

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

### 🏗️ 架构与设计规范

项目采用 DDD + COLA（菱形架构）+ 防腐层（ACL）+ 依赖倒置 + BOM 版本对齐的设计理念，AI 组件作为基础设施适配器通过端口接口与领域层解耦。完整设计文档位于 [`docs/superpowers/specs/`](./docs/superpowers/specs/)：

- **整体架构与组件契约**：[`2026-08-07-ddd4j-ai-architecture-design.md`](./docs/superpowers/specs/2026-08-07-ddd4j-ai-architecture-design.md)（架构理念、模块拓扑、组件状态总表、COLA 分层约定、依赖治理）
- **Core 契约**（已实现）：[`2026-07-01-ai-core-contract-design.md`](./docs/superpowers/specs/2026-07-01-ai-core-contract-design.md)
- **SST 语音**（已实现）：[`2026-01-05-sst-speech-component-design.md`](./docs/superpowers/specs/2026-01-05-sst-speech-component-design.md)
- **全部 AI 组件**（chat / memory / embedding / vectordb / rag / agent / mcp / ocr / flow / router / asr / tts 均已实现）：见 `docs/superpowers/specs/` 同目录各 `2026-08-12-*-design.md`

### 🗺️ 版本路线图

各组件实现遵循分批演进路线，完整实施计划位于 [`docs/superpowers/plans/`](./docs/superpowers/plans/)：

| 版本 | 范围 | 状态 | 计划文档 |
|------|------|------|----------|
| **v1.0** | core 契约 + sst 语音 | ✅ 已完成 | [`2026-08-07-ddd4j-ai-v1-core-and-sst.md`](./docs/superpowers/plans/2026-08-07-ddd4j-ai-v1-core-and-sst.md) |
| **v1.x-A** | chat / memory / embedding / vectordb（LLM 基础层） | ✅ 已完成 | [`2026-08-12-ddd4j-ai-v1x-llm-foundation.md`](./docs/superpowers/plans/2026-08-12-ddd4j-ai-v1x-llm-foundation.md) |
| **v1.x-B** | mcp / ocr / rag / agent（高阶能力） | ✅ 已完成（2026-08-25） | [`2026-08-12-ddd4j-ai-v1x-rag-agent-mcp-ocr.md`](./docs/superpowers/plans/2026-08-12-ddd4j-ai-v1x-rag-agent-mcp-ocr.md) |
| **v2.0** | asr / tts / flow / router（编排与语音独立拆分） | ✅ 已完成（2026-08-25） | [`2026-08-12-ddd4j-ai-v2-orchestration.md`](./docs/superpowers/plans/2026-08-12-ddd4j-ai-v2-orchestration.md) |

### 🔗 相关资源

- [Ddd4j Boot 项目](../ddd4j-boot/README.md)
- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/index.html)
- [Spring AI Alibaba](https://github.com/alibaba/spring-ai-alibaba)
- [Model Context Protocol](https://modelcontextprotocol.io/)
- [Azure Cognitive Speech SDK](https://learn.microsoft.com/azure/ai-services/speech-service/)

### 📄 许可证

本项目采用与 Ddd4j Boot 一致的许可证，详见 [LICENSE](./LICENSE)。
