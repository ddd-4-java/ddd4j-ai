# v1.0 核心契约 + 语音落地（core + sst）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立 ddd4j-ai 的核心契约接口（AiComponent/AiHandler/AiRequest/AiResponse）并实现首个业务组件 SST（Azure Speech + FFmpeg），验证 COLA 分层约定在 AI 组件模块中的可行性。

**Architecture:** core 提供纯 Java 契约（不绑 Spring）；sst 遵循 COLA 分层（dto/vo/enums/properties/service + impl），通过 `SpeechService<T>` 端口接口隔离 Azure SDK，`FFmpegService` 经 ProcessBuilder 调外部 ffmpeg 做格式转换。

**Tech Stack:** Java 17、Maven（`${revision}` flatten，`1.0.0-SNAPSHOT`）、Azure Cognitive Speech SDK 1.47.0、FFmpeg（外部进程）、Spring Boot（sst 的 properties/service 注解）、Lombok、Hutool、Commons Lang3。

**Related Design Doc:** `docs/superpowers/specs/2026-08-07-ddd4j-ai-architecture-design.md`、`docs/superpowers/specs/2026-07-01-ai-core-contract-design.md`、`docs/superpowers/specs/2026-01-05-sst-speech-component-design.md`

> **说明：** 本 plan 记录 v1.0 已完成的历史实现（Task 1–6 标 `- [x]` 并注明对应 git commit），并追加测试与示例补齐待办（Task 7–9 标 `- [ ]`）。历史实现不在本 plan 执行，仅作可追溯记录。

## Global Constraints

- 基线：当前 `master` 分支，HEAD = `3fbd757`（refactor(ai): 重构AI组件模块命名规范，2026-08-07）。
- 历史实现保持行为不变；本 plan 仅新增测试与示例，不改既有 `.java` 源码语义。
- 提交约定：conventional commits（`feat`/`test`/`docs`/`refactor`）。
- 测试框架：JUnit 5 + Mockito（与 Spring Boot starter-test 对齐）。
- FFmpeg 相关测试仅在装有 ffmpeg 的环境执行，CI 无 ffmpeg 时跳过（`Assumptions.assumeTrue`）。

---

## Task 1: SST 模块骨架与 COLA 分层包结构

**Files:**
- Create: `ddd4j-ai-extensions/ddd4j-ai-extension-sst/src/main/java/io/ddd4j/ai/cmpt/sst/{dto,vo,enums,properties,service,service/impl}/package-info.java`
- Create: `ddd4j-ai-extensions/ddd4j-ai-extension-sst/pom.xml`

**Interfaces:** Produces sst 模块拓扑；Consumes ddd4j-ai-core

- [x] **Step 1:** 创建 sst 模块 pom（依赖 core + Azure Speech SDK + Lombok/Hutool/Commons Lang3 + Spring Boot）。
- [x] **Step 2:** 建立 COLA 分层包结构（dto/vo/enums/properties/service/impl）。

> Completed in commit `396adde` (2026-01-05)

---

## Task 2: SST 数据模型与配置（dto/vo/enums/properties）

**Files:**
- Create: `.../cmpt/sst/dto/{STTDto,TTSDto}.java`
- Create: `.../cmpt/sst/vo/{STTResultVO,TTSResultVO}.java`
- Create: `.../cmpt/sst/enums/{AVFormatEnums,ConvertKeyEnums,TTSSTTChannel}.java`
- Create: `.../cmpt/sst/properties/AzureSpeechProperties.java`（`@ConfigurationProperties("azure.speech")` + `@Configuration`，字段 key/region/voiceName/recognitionLanguage）

**Interfaces:** Produces DTO/VO/枚举/配置；Consumes Lombok、Spring Boot

- [x] **Step 1:** 实现 STTDto/TTSDto（入参：channel/formatType/data、text）。
- [x] **Step 2:** 实现 STTResultVO/TTSResultVO（status/msg/text|audio/reason，TTSResultVO 用 Builder）。
- [x] **Step 3:** 实现枚举（TTSSTTChannel.Azure、AVFormatEnums、ConvertKeyEnums）。
- [x] **Step 4:** 实现 AzureSpeechProperties（4 字段 + Lombok）。

> Completed in commit `396adde` (2026-01-05)

---

## Task 3: SST 端口接口、回调与 Azure 适配器实现

**Files:**
- Create: `.../cmpt/sst/service/SpeechService.java`（泛型 `SpeechService<T>`，5 方法）
- Create: `.../cmpt/sst/service/{SpeechServiceText2VoiceCallback,SpeechServiceVoice2TextCallback}.java`
- Create: `.../cmpt/sst/service/impl/AzureSpeechService.java`（implements `SpeechService<SpeechConfig>, InitializingBean`）
- Create: `.../cmpt/sst/service/impl/FFmpegService.java`（ProcessBuilder 管道转换）

**Interfaces:** Produces SpeechService 端口 + Azure 适配器；Consumes Azure Speech SDK 1.47.0、FFmpeg

- [x] **Step 1:** 定义 `SpeechService<T>` 端口（text2Voice/tts/voice2TextFromWavFile/voice2TextFromWavByteArray/voice2TextFromMp3ByteArray）。
- [x] **Step 2:** 定义两个回调接口（onSuccess/onFail[ + onCancel]）。
- [x] **Step 3:** 实现 AzureSpeechService：`afterPropertiesSet` 构建 `SpeechConfig`；tts 输出 `Audio16Khz32KBitRateMonoMp3`；STT 走 `PushAudioInputStream` + `recognizeOnceAsync`，MP3 用 compressed format；统一 `doVoice2Text` 处理 RecognizedSpeech/NoMatch/Canceled。
- [x] **Step 4:** 实现 FFmpegService：convertWavToMp3FromByteAry / convertAudioToWavFromByteAry / convertAudioToWavFromInputStream，非 0 退出码抛异常。

> Completed in commit `396adde` (2026-01-05)

---

## Task 4: Core 契约层抽取

**Files:**
- Create: `ddd4j-ai-core/src/main/java/io/ddd4j/ai/core/{AiComponent,AiHandler,AiRequest,AiResponse,package-info}.java`
- Create: `ddd4j-ai-core/pom.xml`（纯 Java，零 Spring 依赖）

**Interfaces:** Produces AiComponent/AiHandler/AiRequest/AiResponse；Consumes 无（纯 Java）

- [x] **Step 1:** 建立 ddd4j-ai-core 模块（pom 不引 Spring）。
- [x] **Step 2:** 定义 `AiComponent`（`String name()`）。
- [x] **Step 3:** 定义 `AiHandler extends AiComponent`（`AiResponse handle(AiRequest)`）。
- [x] **Step 4:** 定义 `AiRequest` record（input/metadata，紧凑构造器不可变校验 + `of`）。
- [x] **Step 5:** 定义 `AiResponse` record（output/metadata，同上 + `of`）。

> Completed in commit `2b808dc` (2026-07-01)

---

## Task 5: 依赖治理与 BOM 链重构

**Files:**
- Modify: `ddd4j-ai-dependencies/pom.xml`（import ddd4j-boot-dependencies + 追加 AI 专属版本：Spring AI 2.0.0 / Alibaba 2.0.0-M1.1 / MCP 2.0.0 / LangChain4j 1.18.1 / AgentScope 2.0.1 / Speech SDK 1.47.0 / whispercpp 1.4.0 / tts-edge-java 1.3.1 等）
- Modify: `ddd4j-ai-bom/pom.xml`、`ddd4j-ai-parent/pom.xml`

**Interfaces:** Produces 版本治理链；Consumes ddd4j-boot-dependencies

- [x] **Step 1:** 重构 `ddd4j-ai-dependencies`：properties + dependencyManagement（AI BOM 优先声明以覆盖 Boot 旧版本）。
- [x] **Step 2:** 对齐 `ddd4j-ai-bom`（组件版本）与 `ddd4j-ai-parent`（链式 import）。

> Completed in commit `36ef57c` (2026-07-01)

---

## Task 6: 模块命名规范化（架构定型）

**Files:**
- Modify: 各 extension 模块包名 `io.ddd4j.ai.extension.<domain>` 统一；package-info 模板统一（`@version 1.0.0`）
- Modify: 根 pom `<revision>1.0.0-SNAPSHOT</revision>`

**Interfaces:** Produces 当前架构形态；Consumes —

- [x] **Step 1:** 统一组件子包为 `cmpt`（component）约定。
- [x] **Step 2:** 12 个骨架 extension 补齐 package-info 占位 + 版本标注。
- [x] **Step 3:** 重构根 pom revision 与聚合模块声明。

> Completed in commit `3fbd757` (2026-08-07)

---

## Task 7: 为 core 契约补写单元测试（待办）

**Files:**
- Create: `ddd4j-ai-core/src/test/java/io/ddd4j/ai/core/AiRequestTest.java`
- Create: `ddd4j-ai-core/src/test/java/io/ddd4j/ai/core/AiResponseTest.java`
- Create: `ddd4j-ai-core/src/test/java/io/ddd4j/ai/core/AiHandlerContractTest.java`

**Interfaces:** Consumes JUnit 5 + Mockito

- [x] **Step 1:** 写失败测试 —— `AiRequest.of(x)` 等价 `new AiRequest(x, Map.of())`；`input=null` 抛 NPE；`metadata=null` 返回空映射；传入可变 Map 后改动不影响实例（防御性拷贝）。
- [x] **Step 2:** 对称校验 `AiResponse`（output NPE / metadata 不可变 / `of` 工厂）。
- [x] **Step 3:** 用测试桩 `AiHandler` 验证 `handle(AiRequest)` → `AiResponse` 往返，并断言 `name()` 稳定返回。
- [ ] **Step 4:** 全部绿后 `git commit -m "test(ai-core): 补充 core 契约单元测试"`。

> Completed 2026-08-16（19 个用例全绿：AiRequestTest 7 / AiResponseTest 7 / AiHandlerContractTest 5；Step 4 提交待执行）

---

## Task 8: 为 sst 组件补写单元测试（待办）

**Files:**
- Create: `ddd4j-ai-extensions/ddd4j-ai-extension-sst/src/test/java/io/ddd4j/ai/cmpt/sst/properties/AzureSpeechPropertiesTest.java`
- Create: `.../service/SpeechServiceContractTest.java`
- Create: `.../service/impl/AzureSpeechServiceTest.java`
- Create: `.../service/impl/FFmpegServiceTest.java`

**Interfaces:** Consumes JUnit 5 + Mockito + Spring Boot test

- [x] **Step 1:** `AzureSpeechProperties` 的 `@ConfigurationProperties` 绑定测试（`azure.speech.*` → 字段）。
- [x] **Step 2:** 针对端口 `SpeechService<T>` 用测试桩验证回调路径（onSuccess/onFail/onCancel）。
- [x] **Step 3:** mock `SpeechConfig`/`SpeechSynthesizer`/`SpeechRecognizer`，验证 `tts()` 返回结构、`text2Voice()` 回调分支、三入口 `voice2Text*` 与 `doVoice2Text` 的 RecognizedSpeech/NoMatch/Canceled 分支。
- [x] **Step 4:** FFmpegService：命令构造单测；装 ffmpeg 时做端到端转换冒烟，无 ffmpeg 用 `Assumptions.assumeTrue` 跳过。
- [ ] **Step 5:** 全部绿后 `git commit -m "test(sst): 补充 sst 组件单元测试"`。

> Completed 2026-08-16（19 个用例全绿：Properties 3 / 端口契约 4 / FFmpeg 端到端 4 / Azure SDK mock 8）。
> 实施附带修复与发现：
> - **修复**：`ddd4j-ai-extension-sst/pom.xml` 缺失 Spring 依赖（`spring-boot-starter`）导致该模块当前无法编译（存量问题，历史 target/ 为旧产物）；同时补齐 test 依赖（junit-jupiter/assertj/mockito）。
> - **发现（未修，待后续迭代）**：① `tts()` 失败分支返回 `status=1`（与成功相同），建议引入独立失败码；② `FFmpegService.convertAudioToWavFromInputStream` 使用 `redirectErrorStream(true)`，stderr 文本污染输出流，RIFF 头不保证在开头（字节流版为 `false`，不一致）。

---

## Task 9: 补齐 sst 的 sample 示例（待办）

**Files:**
- Create: `ddd4j-ai-samples/src/main/java/io/ddd4j/ai/samples/sst/SstSampleController.java`（或 `SstSampleRunner`）

**Interfaces:** Consumes ddd4j-ai-extension-sst

- [x] **Step 1:** 在 samples 模块新增 sst 端到端示例（配置 + 注入 SpeechService + 调用 tts/voice2Text）。
- [x] **Step 2:** README/示例说明对齐（保留 README 快速开始示例一致）。
- [ ] **Step 3:** `git commit -m "docs(samples): 补充 sst 示例"`。

> Completed 2026-08-16（`SstSample`：TTS 同步/回调 + STT 含 FFmpeg 归一化/MP3 直识别；samples 由 `packaging=pom` 调整为默认 jar 以纳入编译验证）。

---

## Self-Review 结论

- **Spec coverage:** Task 1–3 ↔ `2026-01-05-sst-speech-component-design.md` 第 5/6/7 节；Task 4 ↔ `2026-07-01-ai-core-contract-design.md` 第 5 节；Task 5–6 ↔ `2026-08-07-ddd4j-ai-architecture-design.md` 第 5/9 节；Task 7–8 ↔ 各 spec 第"测试策略"节。✅
- **Placeholder scan:** Task 7–9 为待办，无残留 TODO；历史 Task 1–6 已闭合。✅
- **Type consistency:** `SpeechService<SpeechConfig>` 中 `SpeechConfig` 指 Azure SDK `com.microsoft.cognitiveservices.speech.SpeechConfig`（非项目类），跨 Task 引用一致。✅
