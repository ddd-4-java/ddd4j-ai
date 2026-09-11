package io.ddd4j.ai.extension.agent.dispatch;

/**
 * {@link InMemoryAgentDispatchTaskRepository} 契约测试：与尚未实现的 JDBC 实现
 * 跑**同一套断言**，从而把"语义一致"变成结构保证。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class InMemoryAgentDispatchTaskRepositoryTest extends AgentDispatchTaskRepositoryContract {

    @Override
    protected AgentDispatchTaskRepository newRepository() {
        return new InMemoryAgentDispatchTaskRepository();
    }
}
