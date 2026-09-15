package io.ddd4j.ai.extension.agent.util;

import reactor.core.scheduler.Schedulers;

/**
 * 阻塞调用守卫：把 Reactor 在非阻塞线程上拒绝 {@code .block()} 时抛出的费解报错，
 * 换成带可执行指引的异常。
 *
 * <p>Reactor 只在调用**真的需要等待**时才做 NonBlocking 检查——因此瞬时完成的调用
 * 不会触发它。这也意味着：验证非阻塞行为的测试必须让调用真正耗时，否则会得到
 * 骗人的绿灯。
 *
 * <p>{@link Schedulers#boundedElastic()} 的线程不在 Reactor NonBlocking 集合内，
 * 因此在其上阻塞是合法的，守卫会放行。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public final class BlockingCallGuard {

    private BlockingCallGuard() {
    }

    /**
     * 校验当前线程可以执行阻塞调用。
     *
     * @param asyncAlternative 出问题时提示调用方改用的替代方案
     *                         （如 {@code "executeAsync(task)"} / {@code "dispatchAllAsync(planId)"}）
     * @throws IllegalStateException 当前线程是 Reactor NonBlocking 线程
     */
    public static void requireBlockingCapableThread(String asyncAlternative) {
        if (Schedulers.isInNonBlockingThread()) {
            throw new IllegalStateException(
                    "当前线程 " + Thread.currentThread().getName() + " 是 Reactor 非阻塞线程，"
                            + "不可调用阻塞 API（Reactor 会抛 block() is blocking）。"
                            + "请改用 " + asyncAlternative
                            + "，或在 Schedulers.boundedElastic() 等可阻塞调度器上调用。");
        }
    }
}
