# SST 语音合成/识别组件设计

- 日期：2026-01-05
- 作者：PartMe.AI
- 状态：**已实现**（commit `396adde` feat(sst)；命名规范化 `3fbd757` 2026-08-07）
- 范围：`ddd4j-ai-extension-sst` —— 语音合成（TTS）与语音识别（STT）一体化组件
- 关联文档：整体架构与分层约定见 `2026-08-07-ddd4j-ai-architecture-design.md`；core 契约见 `2026-07-01-ai-core-contract-design.md`

---

## 1. 背景

SST（Speech Synthesis & Transcription）是 ddd4j-ai 最早落地的业务组件（早于 core 契约抽取）。它对接 Azure Cognitive Speech SDK 提供 TTS/STT 能力，并通过外部 FFmpeg 进程做音频格式转换，解决业务系统中语音播报与语音指令识别的需求。

> 命名说明：模块名为 `sst`（Speech Synthesis & Transcription），与独立的 `asr`（WhisperCpp 离线识别）、`tts`（Edge TTS 独立合成）形成互补：sst = 在线 Azure 一体化方案。

## 2. 目标

- 提供**端口接口** `SpeechService<T>`，隔离 Azure SDK，业务层不直接依赖供应商。
- 实现 TTS（文本→语音，同步返回 MP3 / 回调音频流）。
- 实现 STT（WAV 文件 / WAV 字节流 / MP3 字节流 → 文本）。
- 提供 FFmpeg 音频格式转换（任意→16kHz 单声道 WAV、WAV→MP3），作为 STT 前置处理。
- 通过 `@ConfigurationProperties` 暴露 `azure.speech.*` 配置。

### 非目标

- 不实现离线语音识别（由 `ddd4j-ai-extension-asr` 对接 WhisperCpp 承担）。
- 不实现独立免费 TTS（由 `ddd4j-ai-extension-tts` 对接 Edge TTS 承担）。
- 不封装流式长音频识别协议（当前为 `recognizeOnceAsync` 单次识别）。

## 3. 关键决策

| # | 决策 | 理由 |
|---|------|------|
| S1 | `SpeechService<T>` 使用泛型 `T` 绑定供应商语音配置 | Azure 实现绑定为 `com.microsoft.cognitiveservices.speech.SpeechConfig`；未来其他供应商可绑定各自配置类型，端口接口不变 |
| S2 | TTS 提供同步 `tts()` 与回调 `text2Voice()` 两种形态 | 同步便于直接取 MP3 字节流；回调便于流式消费音频数据 |
| S3 | `AzureSpeechService` 实现 `InitializingBean`，在 `afterPropertiesSet` 初始化 `SpeechConfig` | 配置就绪后一次性构建 SDK 配置，避免每次调用重复构造 |
| S4 | STT 统一走 `PushAudioInputStream` + `recognizeOnceAsync` | 字节流/文件统一为推流模型，`close()` 触发发送（注释明确：不 close 会超时） |
| S5 | FFmpeg 通过 `ProcessBuilder` 管道（`pipe:0`/`pipe:1`）处理字节流 | 避免落盘，适合服务端无状态处理；需宿主环境安装 ffmpeg 并在 PATH |
| S6 | 配置类同时标注 `@ConfigurationProperties` + `@Configuration` | 简化引入：业务只需引入组件即生效，无需额外 `@EnableConfigurationProperties` |

## 4. 总体架构

```
业务应用层
   │ 注入 SpeechService<T>（端口）
   ▼
service/SpeechService<T>            ← 端口接口（稳定）
   └── impl/AzureSpeechService       ← Azure 适配器（implements SpeechService<SpeechConfig>）
         └── com.microsoft...SpeechConfig（Azure SDK，泛型实参）
service/impl/FFmpegService           ← 独立音频转换服务（@Service，ProcessBuilder 调 ffmpeg）
properties/AzureSpeechProperties     ← 配置（azure.speech.*）
```

依赖方向：业务 → 端口接口；Azure SDK 仅存在于 impl，不外泄。

## 5. 接口与实现规格

### 5.1 SpeechService<T>（端口）

包 `io.ddd4j.ai.extension.sst.service`：

```java
public interface SpeechService<T> {
    // 文本转语音（回调）
    void text2Voice(String content, SpeechServiceText2VoiceCallback callback) throws Exception;
    // 文本转语音（同步，返回 TTSResultVO，含 MP3 字节流）
    TTSResultVO tts(T speechConfigDto, String content) throws Exception;
    // 语音转文本：WAV 文件路径
    void voice2TextFromWavFile(String wavFile, SpeechServiceVoice2TextCallback callback) throws Exception;
    // 语音转文本：WAV 字节流
    void voice2TextFromWavByteArray(T speechConfigDto, byte[] wavFileBytes, SpeechServiceVoice2TextCallback callback) throws Exception;
    // 语音转文本：MP3 字节流（内部用 compressed MP3 音频流格式）
    void voice2TextFromMp3ByteArray(T speechConfigDto, byte[] wavFileBytes, SpeechServiceVoice2TextCallback callback) throws Exception;
}
```

回调接口：
- `SpeechServiceText2VoiceCallback`：`onSuccess(byte[] audioData)` / `onFail(String msg)`。
- `SpeechServiceVoice2TextCallback`：`onSuccess(String text)` / `onFail(String msg)` / `onCancel(String msg)`。

### 5.2 AzureSpeechService（实现）

包 `io.ddd4j.ai.extension.sst.service.impl`：

```java
@Slf4j @Component
public class AzureSpeechService implements SpeechService<SpeechConfig>, InitializingBean {
    // afterPropertiesSet(): 由 AzureSpeechProperties 构建 SpeechConfig（key/region/voiceName/recognitionLanguage）
    // tts(): 默认输出 Audio16Khz32KBitRateMonoMp3，成功返回 TTSResultVO{status=1, msg=成功, audio=mp3}
    // text2Voice(): 合成成功回调 onSuccess(audioData)，取消回调 onFail(errorDetails)
    // voice2Text*: PushAudioInputStream 写入后 close 触发识别；doVoice2Text 统一处理 RecognizedSpeech/NoMatch/Canceled
}
```

- `speechConfigDto` 参数为 null 时回退到 `afterPropertiesSet` 初始化的默认配置。
- MP3 识别走 `AudioStreamFormat.getCompressedFormat(AudioStreamContainerFormat.MP3)`。
- 错误处理：`Canceled` 时记录 ErrorCode/ErrorDetails 并回调 `onCancel`/`onFail`。

### 5.3 FFmpegService

包 `io.ddd4j.ai.extension.sst.service.impl`，`@Service`：

```java
public class FFmpegService {
    // WAV → MP3（192k, 44100Hz, 单声道）
    byte[] convertWavToMp3FromByteAry(byte[] sourceAry);
    // 任意音频 → 16kHz 单声道 WAV（Azure STT 推荐格式）
    byte[] convertAudioToWavFromByteAry(byte[] sourceAry);
    // 任意音频输入流 → 16kHz 单声道 WAV
    byte[] convertAudioToWavFromInputStream(InputStream inputStream);
}
```

- 通过 `ProcessBuilder` 启动 `ffmpeg`（`ffmpegBin="ffmpeg"`），stdin/stdout 管道传输。
- 退出码非 0 抛 `RuntimeException("FFmpeg进程执行失败，退出代码：" + exitCode)`。

### 5.4 AzureSpeechProperties（配置）

包 `io.ddd4j.ai.extension.sst.properties`：

```java
@ConfigurationProperties(prefix = "azure.speech")
@Configuration
@Getter @Setter
public class AzureSpeechProperties {
    private String key;                 // Azure Speech 订阅密钥
    private String region;              // 服务区域，如 eastasia
    private String voiceName;           // TTS 音色，如 zh-CN-XiaoxiaoNeural
    private String recognitionLanguage; // STT 识别语言，如 zh-CN
}
```

配置示例（`application.yml`）：

```yaml
azure:
  speech:
    key: ${AZURE_SPEECH_KEY}
    region: eastasia
    voice-name: zh-CN-XiaoxiaoNeural
    recognition-language: zh-CN
```

## 6. 数据模型（dto / vo / enums）

包 `io.ddd4j.ai.extension.sst`：

| 类型 | 类名 | 说明 |
|------|------|------|
| 请求 DTO | `STTDto` | 语音转文本入参（channel、formatType、data） |
| 请求 DTO | `TTSDto` | 文本转语音入参（text） |
| 响应 VO | `STTResultVO` | 识别结果（status、msg、text、reason） |
| 响应 VO | `TTSResultVO` | 合成结果（status、msg、audio、reason，Builder 模式） |
| 渠道枚举 | `TTSSTTChannel` | 当前支持 `Azure` |
| 音频格式 | `AVFormatEnums` | 音频采样率与编码格式枚举 |
| 转换键 | `ConvertKeyEnums` | FFmpeg 转换参数键 |

## 7. 模块落点

```
ddd4j-ai-extension-sst/src/main/java/io/ddd4j/ai/cmpt/sst
├── dto/   {STTDto, TTSDto}
├── vo/    {STTResultVO, TTSResultVO}
├── enums/ {AVFormatEnums, ConvertKeyEnums, TTSSTTChannel}
├── properties/ {AzureSpeechProperties}
└── service/
    ├── SpeechService.java
    ├── SpeechServiceText2VoiceCallback.java
    ├── SpeechServiceVoice2TextCallback.java
    └── impl/ {AzureSpeechService, FFmpegService}
```

## 8. 运行时与依赖

- 第三方依赖：`com.microsoft.cognitiveservices.speech:client-sdk:1.47.0`（BOM 治理）、Lombok、Hutool（`FileUtil`）、Apache Commons Lang3（`StringUtils`）、Spring Boot（properties/service 注解）。
- 外部进程：FFmpeg 必须在宿主 PATH；缺失时 `FFmpegService` 转换调用会抛异常。
- 配置缺失：`AzureSpeechService.afterPropertiesSet` 在 key/region 缺失时仍构建（SDK 层报错），建议业务侧通过 `${AZURE_SPEECH_KEY}` 注入。

## 9. 错误处理与降级

- TTS `Canceled`：记录 `ErrorCode/ErrorDetails`，回调 `onFail`；同步 `tts()` 返回 `TTSResultVO{status=1, msg=失败, audio=null}`。
- STT `NoMatch`：回调 `onFail(reason.name())`（语音不能被识别）。
- STT `Canceled`：回调 `onCancel(errorDetails)`。
- FFmpeg 失败：非 0 退出码抛 `RuntimeException`，由业务层捕获。

## 10. 测试策略

> 现状：sst 当前零测试。补齐计划见 `2026-08-07-ddd4j-ai-v1-core-and-sst.md`。

- **端口契约测试**：针对 `SpeechService<T>` 接口，用测试桩验证回调路径（onSuccess/onFail/onCancel）。
- **AzureSpeechService**：mock `SpeechConfig`/`SpeechSynthesizer`/`SpeechRecognizer`，验证 tts 返回结构、text2Voice 回调分支、voice2Text* 三入口与 doVoice2Text 的三 reason 分支。
- **FFmpegService**：在装有 ffmpeg 的 CI 环境做端到端转换冒烟；无 ffmpeg 环境做命令构造与异常路径单测。
- **AzureSpeechProperties**：`@ConfigurationProperties` 绑定测试，校验 `azure.speech.*` → 字段映射。
