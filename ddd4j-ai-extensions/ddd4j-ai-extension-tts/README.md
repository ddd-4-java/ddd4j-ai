# ddd4j-ai-extension-tts

> 高性能流式 TTS 组件：Edge TTS 免费兜底 + DashScope Qwen3-TTS Realtime 高性能后端（TTFA ~97ms）， + 自动降级路由。

## 设计哲学

本扩展的设计哲学借鉴自 [agentscope-cpp 的 TTS 技术决策](../workspace-tinyclaw-ai/agentscope-cpp/docs/superpowers/plans/2026-09-10-tts-tech-decision.md)，核心要点：

1. **TTFA（Time-to-First-Audio）优先于总合成时长** — 对话式 AI 的"听感延迟"由首块决定，而不是"全部说完的时间"。
2. **流式文本输入 → 流式音频输出** — 不等 LLM 生成完整回答，按文本增量分批送 TTS。
3. **WebSocket 长连接 vs 一次性 HTTP** — DashScope Realtime API 通过长连接维持会话状态，TTFA ~97ms（一次性 HTTP 调用 Edge TTS 约 500-1500ms）。
4. **TextChunker 按标点边界切分** — max=24 chars / min=1 char，避免 TTS 在词中被截断产生碎片音频。
5. **三级降级路由** — 在线 API → 本地兜底，主后端不可用时自动降级。

## 架构

```
                ┌─────────────────────────────────────┐
   LLM/Agent    │  TextDeltaTtsBridge (桥接层)        │    ┌──────────────────────┐
   文本增量 ──► │  • 订阅 AgentEvent                  │───►│  TtsRouter (路由器)   │
                │  • TextChunker 标点切分             │    │  primary → fallback │
                │  • 切分 chunks → TTS                │    └──────────┬───────────┘
                └─────────────────────────────────────┘               │
                                                                     ▼
                                            ┌─────────────────────────────────────┐
                                            │  DashScopeRealtimeTtsService        │
                                            │  • WebSocket 长连接                 │
                                            │  • TTFA 97ms（业界领先）            │
                                            │  • 真正流式（多个音频 chunk）       │
                                            └──────────────┬──────────────────────┘
                                                           │ 失败自动降级
                                                           ▼
                                            ┌─────────────────────────────────────┐
                                            │  EdgeTtsService（兜底后端）         │
                                            │  • whitemagic tts-edge-java         │
                                            │  • 整段合成（mp3 单元素流）         │
                                            │  • TTFA ~500-1500ms（已知上限）    │
                                            └─────────────────────────────────────┘
```

## 接入步骤

### 1. 引入依赖

```xml
<dependency>
    <groupId>io.ddd4j.ai</groupId>
    <artifactId>ddd4j-ai-extension-tts</artifactId>
    <version>${ddd4j.ai.version}</version>
</dependency>
```

### 2. 配置 application.yml

```yaml
ddd4j:
  ai:
    tts:
      enabled: true
      primary: dashscope          # 主后端；dashscope / edge
      edge:
        enabled: true
        default-voice: zh-CN-XiaoxiaoNeural
      dashscope:
        enabled: true
        api-key: ${DASHSCOPE_API_KEY}    # 阿里云百炼控制台申请
        model: qwen3-tts-12hz-0.6b-customvoice
        voice: Cherry
      chunker:
        max-chars: 24             # 与 agentscope-cpp TextChunker 对齐
        min-chars: 1
      metrics:
        enabled: true             # TTFA 自维护采样
```

### 3. 业务方使用

```java
@Component
public class MyVoiceService {
    private final TtsRouter ttsRouter;

    public MyVoiceService(TtsRouter ttsRouter) {
        this.ttsRouter = ttsRouter;
    }

    /** 直接流式合成（DashScope 真正流式 / Edge 单元素流）。 */
    public Flux<byte[]> streamSpeech(String text, String voice) {
        return ttsRouter.streamSynthesize(text, voice);
    }

    /** 阻塞整段合成（向后兼容）。 */
    public byte[] synthesize(String text, String voice) throws Exception {
        return ttsRouter.synthesize(text, voice);
    }

    /** 与 Agent 事件流对接：见 TextDeltaTtsBridge（agentscope-cpp StreamFirstChunkMiddleware 对应物）。*/
    public Flux<byte[]> pipeAgentTextDeltas(Flux<AgentEvent> events, String voice) {
        TextDeltaTtsBridge bridge = new TextDeltaTtsBridge(
                TextChunker.create(24, 1), ttsRouter);
        return bridge.pipe(events, voice);
    }
}
```

## 性能对比

| 后端 | TTFA | 流式 | API key | 推荐场景 |
|------|------|------|---------|---------|
| **DashScope Qwen3-TTS Realtime** | **~97ms** ⭐ | ✅ 真正流式 | 需要（阿里云百炼） | **生产首选** |
| Edge TTS | 500-1500ms | ❌ 单元素流 | 免费 | 开发兜底 / 弱网 |

> 数据来源：agentscope-cpp `2026-09-10-tts-tech-decision.md` §1.1 横向对比表。

## 端口契约

```java
public interface TtsService {
    byte[] synthesize(String text, String voice) throws Exception;
    Flux<byte[]> streamSynthesize(String text, String voice);
}

public interface TtsRouter extends TtsService {
    List<String> backendNames();   // 当前装配的后端链路
}
```

`TtsRouter` 继承 `TtsService`，业务方可统一注入（推荐）或按需注入命名 Bean（`edgeTtsService` / `dashScopeRealtimeTtsService`）。

## TTFA 指标

不依赖 Micrometer / actuator，自维护采样器：

```java
TtsMetrics metrics = = new TtsMetrics();
// ... 业务方调用 TTS 后
metrics.recordTtfa(elapsedNanos);
metrics.logSummary(logger);
// 输出：tts.firstaudio: samples=42, avg=97ms, min=80ms, max=210ms
```

业务方需要 Prometheus/OTEL 时自行注入埋点。

## 与 agentscope-cpp 的对偶设计

| agentscope-cpp | ddd4j-ai |
|----------------|----------|
| `StreamFirstChunkMiddleware` | `TextDeltaTtsBridge` |
| `TextChunker` (max=24) | `TextChunker` (max=24) |
| `Qwen3TtsRealtimeClient` (WS) | `DashScopeRealtimeTtsService` + `DashScopeRealtimeClient` |
| `LocalTtsFallback` | `EdgeTtsService` |
| `TtsRouter`（三级路由）| `FallbackTtsRouter`（primary → fallback）|
| TTFA 指标（profiler）| `TtsMetrics`（自维护 + Logger）|

## 不在 v1 范围

- 多语言自动切换 / 情感参数控制
- 音频格式转换（DashScope 输出 PCM，Edge 输出 mp3；上层播放器自行处理）
- 与 `Flow` 工作流节点集成（TTS 节点类型规划中）
- Micrometer / actuator 集成（按需引入）

## 已知限制

- Edge TTS 受 whitemagic 库限制，整段合成后才返回 mp3 字节，`streamSynthesize` 实际是单元素 Flux。
- DashScope 后端在 WebSocket 断开 / API 限流时会自动降级到 Edge，但**中途音频格式混杂**——上层播放器应能容忍。