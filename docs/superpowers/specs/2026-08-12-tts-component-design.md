# TTS 文本转语音组件设计

- 日期：2026-08-12
- 作者：PartMe.AI
- 状态：待实施（骨架：仅 package-info 占位）
- 范围：`ddd4j-ai-extension-tts` —— 独立文本转语音（免费在线方案）
- 关联文档：整体架构见 `2026-08-07-ddd4j-ai-architecture-design.md`；与 sst 的 Azure TTS 互补

---

## 1. 背景

sst 的 TTS 基于 Azure Cognitive Speech（付费、高质量）。tts 组件规划对接 Edge TTS（`tts-edge-java:1.3.1`），提供**免费在线**文本转语音，满足低成本/非关键场景，与 sst 的 Azure TTS 形成付费/免费互补。

## 2. 目标

- 集成 tts-edge-java 1.3.1 提供免费在线 TTS。
- 提供音色管理（多语种/多音色）。
- 预留流式合成扩展。

### 非目标

- 不替代 sst 的 Azure TTS（二者并存，业务按质量/成本选）。
- 不实现离线 TTS。

## 3. 关键决策（建议）

| # | 决策候选 | 理由 |
|---|---------|------|
| T1 | 对接 tts-edge-java 1.3.1（BOM 已声明） | 免费、多语种 |
| T2 | 端口接口与 sst TTS 语义对齐 | 业务可平滑切换 |
| T3 | 音色枚举可配置 | Edge TTS 音色频繁更新 |

## 4. 总体架构

```
业务 → TtsService（端口）→ impl/EdgeTtsService → tts-edge-java
```

## 5. 接口方向

- `TtsService`：`synth(String text, String voice)` → `byte[]`（音频）；`synthStream(...)`。

## 6. 模块落点

```
ddd4j-ai-extension-tts/src/main/java/io/ddd4j/ai/cmpt/tts
├── dto/vo/enums/ (VoiceEnum)
├── properties/ (EdgeTtsProperties: 默认音色/格式)
├── service/TtsService.java
└── service/impl/EdgeTtsService.java
```

## 7. 依赖

- tts-edge-java 1.3.1（`tts-edge-java.version`）

## 8. 测试策略

- 端口契约测试；样例文本合成冒烟（需网络）；音色枚举有效性校验。
