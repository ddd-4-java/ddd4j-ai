# ASR 自动语音识别组件设计

- 日期：2026-08-12
- 作者：PartMe.AI
- 状态：待实施（骨架：仅 package-info 占位）
- 范围：`ddd4j-ai-extension-asr` —— 离线/本地自动语音识别
- 关联文档：整体架构见 `2026-08-07-ddd4j-ai-architecture-design.md`；与 sst 在线方案互补

---

## 1. 背景

sst 组件的 Azure Speech 是在线云方案，依赖网络与订阅。asr 组件规划对接 WhisperCpp（`whispercpp:1.4.0`）提供**离线/本地**语音识别，满足数据合规、离线、低成本场景，与 sst 形成在线/离线互补。

## 2. 目标

- 集成 WhisperCpp 1.4.0 提供离线语音识别。
- 提供音频预处理管道（可复用 sst 的 FFmpegService 做格式归一化）。
- 提供模型管理（模型文件路径/语言）。

### 非目标

- 不替代 sst 的在线 Azure 方案（二者并存，业务按场景选）。
- 不实现 TTS（由 tts 组件承担）。

## 3. 关键决策（建议）

| # | 决策候选 | 理由 |
|---|---------|------|
| ASR1 | 对接 whispercpp 1.4.0（BOM 已声明） | 离线、免费 |
| ASR2 | 输入统一为 16kHz 单声道 WAV（复用 FFmpegService） | Whisper 推荐格式 |
| ASR3 | 端口接口与 sst STT 语义对齐 | 业务可平滑切换在线/离线 |

## 4. 总体架构

```
业务 → AsrService（端口）→ impl/WhisperAsrService → whispercpp
                              ↑（可选）FFmpegService 格式归一化
```

## 5. 接口方向

- `AsrService`：`recognize(byte[] wav)` → `String`；`recognizeFile(String path)`。

## 6. 模块落点

```
ddd4j-ai-extension-asr/src/main/java/io/ddd4j/ai/cmpt/asr
├── properties/ (WhisperProperties: 模型路径/语言)
├── service/AsrService.java
└── service/impl/WhisperAsrService.java
```

## 7. 依赖

- whispercpp 1.4.0（`whispercpp.version`）
- 可选：复用 sst 的 FFmpegService（跨组件依赖或内聚一份）

## 8. 测试策略

- 端口契约测试；样例音频识别冒烟（需模型文件）；空/噪声输入边界测试。
