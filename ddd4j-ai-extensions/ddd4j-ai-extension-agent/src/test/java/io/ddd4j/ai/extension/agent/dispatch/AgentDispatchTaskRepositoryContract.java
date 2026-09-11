package io.ddd4j.ai.extension.agent.dispatch;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AgentDispatchTaskRepository} 共享契约测试基类。
 *
 * <p>子类仅需提供实现实例（{@link #newRepository()}），断言在此统一维护——
 * 从而以**结构**保证"内存实现与各持久化实现语义一致"，而非靠人工同步。
 *
 * <p>锚定两条容易漂移的语义：
 * <ul>
 *   <li>{@code findByPlanId} 按 {@code createdAt} **升序**返回</li>
 *   <li>{@code save} 是**按 id 幂等的 upsert**（与内存实现的 {@code put} 一致，
 *        {@code dispatchAll} 的重试路径依赖此语义）</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public abstract class AgentDispatchTaskRepositoryContract {

    /** 每个用例一个新实例（避免用例间状态串扰）。 */
    protected abstract AgentDispatchTaskRepository newRepository();

    private static AgentDispatchTask task(String id, String planId, String instruction, Instant createdAt) {
        return new AgentDispatchTask(id, planId, instruction, AgentDispatchTask.PENDING, null, createdAt);
    }

    @Test
    void saveThenFindById_returnsSameContent() {
        AgentDispatchTaskRepository repository = newRepository();
        Instant now = Instant.parse("2026-09-11T10:00:00Z");
        repository.save(task("t1", "p1", "boil water", now));

        Optional<AgentDispatchTask> found = repository.findById("t1");

        assertThat(found).isPresent();
        assertThat(found.get().planId()).isEqualTo("p1");
        assertThat(found.get().instruction()).isEqualTo("boil water");
        assertThat(found.get().status()).isEqualTo(AgentDispatchTask.PENDING);
        assertThat(found.get().createdAt()).isEqualTo(now);
    }

    @Test
    void findById_unknownId_returnsEmpty() {
        assertThat(newRepository().findById("nope")).isEmpty();
    }

    @Test
    void findByPlanId_returnsOnlyThatPlan_inCreatedAtOrder() {
        AgentDispatchTaskRepository repository = newRepository();
        // 故意乱序写入，断言按 createdAt 升序返回
        repository.save(task("t2", "p1", "second", Instant.parse("2026-09-11T10:00:02Z")));
        repository.save(task("t1", "p1", "first", Instant.parse("2026-09-11T10:00:01Z")));
        repository.save(task("t3", "p1", "third", Instant.parse("2026-09-11T10:00:03Z")));
        repository.save(task("x1", "p2", "other plan", Instant.parse("2026-09-11T10:00:00Z")));

        List<AgentDispatchTask> tasks = repository.findByPlanId("p1");

        assertThat(tasks).extracting(AgentDispatchTask::id).containsExactly("t1", "t2", "t3");
    }

    @Test
    void findByPlanId_unknownPlan_returnsEmptyList() {
        assertThat(newRepository().findByPlanId("nope")).isEmpty();
    }

    @Test
    void update_overwritesStatusAndResult() {
        AgentDispatchTaskRepository repository = newRepository();
        repository.save(task("t1", "p1", "do it", Instant.parse("2026-09-11T10:00:00Z")));

        repository.update(task("t1", "p1", "do it", Instant.parse("2026-09-11T10:00:00Z"))
                .withStatus(AgentDispatchTask.DONE, "finished"));

        AgentDispatchTask found = repository.findById("t1").orElseThrow();
        assertThat(found.status()).isEqualTo(AgentDispatchTask.DONE);
        assertThat(found.result()).isEqualTo("finished");
    }

    @Test
    void save_sameIdTwice_isUpsert() {
        AgentDispatchTaskRepository repository = newRepository();
        Instant now = Instant.parse("2026-09-11T10:00:00Z");
        repository.save(task("t1", "p1", "v1", now));
        repository.save(task("t1", "p1", "v2", now));

        assertThat(repository.findById("t1").orElseThrow().instruction()).isEqualTo("v2");
        assertThat(repository.findByPlanId("p1")).hasSize(1);
    }

    @Test
    void multiplePlansDoNotBleed() {
        AgentDispatchTaskRepository repository = newRepository();
        repository.save(task("a1", "planA", "a", Instant.parse("2026-09-11T10:00:00Z")));
        repository.save(task("b1", "planB", "b", Instant.parse("2026-09-11T10:00:00Z")));

        assertThat(repository.findByPlanId("planA")).extracting(AgentDispatchTask::id).containsExactly("a1");
        assertThat(repository.findByPlanId("planB")).extracting(AgentDispatchTask::id).containsExactly("b1");
    }
}
