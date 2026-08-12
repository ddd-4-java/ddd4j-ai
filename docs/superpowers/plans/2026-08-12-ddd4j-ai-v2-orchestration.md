# v2.0 编排能力与语音独立拆分（asr / tts / flow / router）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现离线语音识别（ASR）、独立文本转语音（TTS）、工作流编排（Flow）、多模型智能路由（Router），完成 ddd4j-ai 全组件覆盖。

**Architecture:** flow 对接 Spring AI Alibaba Graph；router 提供多模型路由策略；asr 对接 WhisperCpp（离线）；tts 对接 Edge TTS（免费在线），与 sst 的 Azure 在线方案互补。

**Tech Stack:** Java 17、Maven、Spring AI Alibaba Graph 2.0.0-M1.1、WhisperCpp 1.4.0、tts-edge-java 1.3.1、Spring AI 2.0.0。

**Related Design Doc:** `docs/superpowers/specs/2026-08-12-asr-component-design.md`、`2026-08-12-tts-component-design.md`、`2026-08-12-flow-component-design.md`、`2026-08-12-router-component-design.md`；架构基线 `2026-08-07-ddd4j-ai-architecture-design.md`。

## Global Constraints

- 前置依赖：v1.x-A/B 基础与高阶组件就绪（flow/router 依赖 chat/agent）。
- 语音组件（asr/tts）端口语义与 sst 对齐，便于业务平滑切换在线/离线。
- TDD；每组件独立模块；不侵入 core。
- 提交约定：conventional commits。

---

## Task 1: 实现 ddd4j-ai-extension-asr

**Files:**
- Create: `.../cmpt/asr/service/AsrService.java`、`service/impl/WhisperAsrService.java`、`properties/WhisperProperties.java`
- Test: `.../cmpt/asr/service/AsrServiceContractTest.java`、`impl/WhisperAsrServiceTest.java`

**Interfaces:** Produces AsrService；Consumes whispercpp 1.4.0（可选复用 sst FFmpegService）

- [ ] **Step 1:** 写失败测试 —— `recognize(wav)` 返回文本；空/噪声边界；输入格式归一化（16kHz 单声道 WAV）。
- [ ] **Step 2:** 定义 AsrService 端口（`recognize(byte[])`/`recognizeFile(String)`）。
- [ ] **Step 3:** 实现 WhisperAsrService 对接 whispercpp；模型路径/语言走 WhisperProperties。
- [ ] **Step 4:** 样例音频冒烟（需模型文件，缺失则 `Assumptions.assumeTrue` 跳过）。
- [ ] **Step 5:** 绿后 `git commit -m "feat(asr): 实现 WhisperCpp 离线语音识别组件"`。

---

## Task 2: 实现 ddd4j-ai-extension-tts

**Files:**
- Create: `.../cmpt/tts/service/TtsService.java`、`service/impl/EdgeTtsService.java`、`enums/VoiceEnum.java`、`properties/EdgeTtsProperties.java`
- Test: `.../cmpt/tts/service/TtsServiceContractTest.java`、`impl/EdgeTtsServiceTest.java`

**Interfaces:** Produces TtsService；Consumes tts-edge-java 1.3.1

- [ ] **Step 1:** 写失败测试 —— `synth(text,voice)` 返回音频字节；音色枚举有效性；流式扩展点。
- [ ] **Step 2:** 定义 TtsService 端口（`synth`/`synthStream`）。
- [ ] **Step 3:** 实现 EdgeTtsService 对接 tts-edge-java；VoiceEnum 可配置。
- [ ] **Step 4:** 样例合成冒烟（需网络）。
- [ ] **Step 5:** 绿后 `git commit -m "feat(tts): 实现 Edge TTS 文本转语音组件"`。

---

## Task 3: 实现 ddd4j-ai-extension-flow

**Files:**
- Create: `.../cmpt/flow/dto/{FlowDef,FlowResult,NodeDef}.java`、`service/FlowService.java`、`service/impl/GraphFlowEngine.java`、`properties/FlowProperties.java`
- Test: `.../cmpt/flow/service/FlowServiceTest.java`

**Interfaces:** Produces FlowService；Consumes spring-ai-alibaba-graph-core 2.0.0-M1.1、chat/agent/memory

- [ ] **Step 1:** 写失败测试 —— 流程定义解析；分支/循环节点执行；节点调用 chat/agent。
- [ ] **Step 2:** 定义 FlowService 端口与节点抽象（LLM/Tool/Branch/Human）。
- [ ] **Step 3:** 实现 GraphFlowEngine 对接 Alibaba Graph；支持声明式流程定义。
- [ ] **Step 4:** 端到端示例流程冒烟。
- [ ] **Step 5:** 绿后 `git commit -m "feat(flow): 实现工作流编排组件"`。

---

## Task 4: 实现 ddd4j-ai-extension-router

**Files:**
- Create: `.../cmpt/router/dto/{ModelSpec,RoutePolicy}.java`、`enums/...`、`service/RouterService.java`、`service/impl/{RoundRobin,Weighted,Cost,Latency}Router.java`、`properties/RouterProperties.java`
- Test: `.../cmpt/router/service/RouterServiceTest.java`

**Interfaces:** Produces RouterService；Consumes chat

- [ ] **Step 1:** 写失败测试 —— 各策略选模型；故障转移；并发下权重分布；指标采集。
- [ ] **Step 2:** 定义 RouterService 端口 + 路由策略接口。
- [ ] **Step 3:** 实现 RoundRobin/Weighted/Cost/Latency 策略；健康检查 + 熔断。
- [ ] **Step 4:** RouterProperties（策略/权重/熔断阈值）。
- [ ] **Step 5:** 绿后 `git commit -m "feat(router): 实现多模型智能路由组件"`。

---

## Task 5: 集成验证 + samples 总补齐

**Files:**
- Test: `ddd4j-ai-samples/src/test/.../V2SmokeIT.java`
- Create: `ddd4j-ai-samples/.../samples/{asr,tts,flow,router}/...`

- [ ] **Step 1:** 写联合冒烟：router 选模型 → flow 编排 → asr/tts 语音闭环。
- [ ] **Step 2:** 补齐本批四组件 sample 示例。
- [ ] **Step 3:** 绿后 `git commit -m "test(samples): v2.0 编排与语音联合冒烟及示例"`。

---

## Self-Review 结论

- **Spec coverage:** Task 1–4 一一对应四份组件 spec；Task 5 覆盖集成与 samples。✅
- **Placeholder scan:** 无 TODO/TBD。✅
- **Type consistency:** asr/tts 端口语义与 sst 对齐（在线/离线可切换）；router 与 chat 衔接一致。✅
