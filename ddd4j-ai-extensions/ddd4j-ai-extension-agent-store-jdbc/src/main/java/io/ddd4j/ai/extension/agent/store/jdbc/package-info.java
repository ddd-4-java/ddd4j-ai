/**
 * 智能体派发任务仓储的 JDBC 实现：以 {@code DataSource} 为接入点，
 * 一次覆盖 PostgreSQL / MySQL / H2。
 *
 * <p>本模块是**可选构件**——只有需要派发任务持久化的业务才引入，
 * 从而不把 {@code spring-jdbc} 传染给所有智能体使用方。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
package io.ddd4j.ai.extension.agent.store.jdbc;
