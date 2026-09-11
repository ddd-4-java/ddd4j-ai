package io.ddd4j.ai.extension.agent.util;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link BlockingCallGuard} 测试：非阻塞线程上抛出带指引的异常，可阻塞线程上放行。
 *
 * <p>注意：本模块 classpath 无 {@code reactor-test}，故断言用 {@code .block()} + AssertJ
 * 而非 {@code StepVerifier}，避免为测试新增依赖。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class BlockingCallGuardTest {

    @Test
    void allowsOnBoundedElasticThread() {
        // boundedElastic 是 Reactor 专为包装阻塞调用设计的调度器，守卫必须放行
        String result = Mono.fromCallable(() -> {
                    BlockingCallGuard.requireBlockingCapableThread("executeAsync(task)");
                    return "ok";
                })
                .subscribeOn(Schedulers.boundedElastic())
                .block();

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void allowsOnPlainCallerThread() {
        // 普通调用方线程（非 NonBlocking）应放行——不抛异常即通过
        BlockingCallGuard.requireBlockingCapableThread("executeAsync(task)");
    }

    @Test
    void throwsOnParallelThreadWithActionableMessage() {
        assertThatThrownBy(() -> Mono.fromCallable(() -> {
                    BlockingCallGuard.requireBlockingCapableThread("executeAsync(task)");
                    return "unreachable";
                })
                .subscribeOn(Schedulers.parallel())
                .block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("非阻塞线程")
                .hasMessageContaining("executeAsync(task)");
    }
}
