package io.ddd4j.ai.extension.tts.router.impl;

import io.ddd4j.ai.extension.tts.router.TtsRouter;
import io.ddd4j.ai.extension.tts.service.TtsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 按 primary → fallback 顺序尝试合成的 {@link TtsRouter}：当前一个后端以错误终止流时，
 * 自动切换到链路的下一个后端，全部失败则返回 {@link IllegalStateException}。
 *
 * <p>设计要点：
 * <ul>
 *   <li>链路在构造时按 {@link #backendNames} 顺序固化（LinkedHashMap → List）。</li>
 *   <li>每个后端的合成流仅在"还没发出任何字节且首信号为错误"时才触发降级，避免音频混杂。</li>
 *   <li>中途失败的降级不在 v1 范围（业务方应避免长音频流中途切换导致格式不一致）。</li>
 * </ul>
 *
 * <p>配置示例：
 * <pre>{@code
 * Map<String, TtsService> backends = new LinkedHashMap<>();
 * backends.put("dashscope", dashScopeService);  // primary
 * backends.put("edge", edgeService);            // fallback
 * TtsRouter router = new FallbackTtsRouter(backends);
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class FallbackTtsRouter implements TtsRouter {

    private static final Logger log = LoggerFactory.getLogger(FallbackTtsRouter.class);

    private final List<String> backendNames;
    private final List<TtsService> backends;

    public FallbackTtsRouter(Map<String, TtsService> backends) {
        Objects.requireNonNull(backends, "backends");
        if (backends.isEmpty()) {
            throw new IllegalArgumentException("backends must not be empty");
        }
        this.backendNames = List.copyOf(backends.keySet());
        this.backends = List.copyOf(backends.values());
    }

    @Override
    public List<String> backendNames() {
        return backendNames;
    }

    @Override
    public byte[] synthesize(String text, String voice) throws Exception {
        List<Exception> failures = new ArrayList<>();
        for (TtsService backend : backends) {
            try {
                return backend.synthesize(text, voice);
            } catch (Exception ex) {
                log.warn("TTS 后端 {} 合成失败: {}", backend, ex.getMessage());
                failures.add(ex);
            }
        }
        IllegalStateException ex = new IllegalStateException("所有 TTS 后端均不可用");
        for (Exception f : failures) {
            ex.addSuppressed(f);
        }
        throw ex;
    }

    @Override
    public Flux<byte[]> streamSynthesize(String text, String voice) {
        return streamWithFallback(backends.iterator(), text, voice, new ArrayList<>());
    }

    private Flux<byte[]> streamWithFallback(Iterator<TtsService> it, String text, String voice,
                                            List<Throwable> failures) {
        if (!it.hasNext()) {
            IllegalStateException ex = new IllegalStateException("所有 TTS 后端均不可用");
            failures.forEach(ex::addSuppressed);
            return Flux.error(ex);
        }
        TtsService current = it.next();
        return current.streamSynthesize(text, voice)
                .switchOnFirst((signal, flux) -> {
                    if (signal.hasError()) {
                        Throwable err = signal.getThrowable();
                        log.warn("TTS 后端 {} 流式失败，降级到下一个: {}", current, err.getMessage());
                        failures.add(err);
                        return streamWithFallback(it, text, voice, failures);
                    }
                    return flux;
                });
    }

    /** 公开构造器（无后端顺序保证），仅供测试。 */
    public static FallbackTtsRouter of(TtsService... backends) {
        java.util.LinkedHashMap<String, TtsService> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < backends.length; i++) {
            map.put("backend-" + i, backends[i]);
        }
        return new FallbackTtsRouter(Collections.unmodifiableMap(map));
    }
}