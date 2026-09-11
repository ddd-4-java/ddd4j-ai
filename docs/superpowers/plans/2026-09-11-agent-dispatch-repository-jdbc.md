# Agent 派发任务仓储 JDBC 持久化实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增 `ddd4j-ai-extension-agent-store-jdbc` 扩展，为 `AgentDispatchTaskRepository` 提供 JDBC 实现，一次覆盖 PostgreSQL / MySQL / H2，消除"进程重启即丢派发任务"的缺陷。

**Architecture:** 独立扩展模块（避免 `spring-jdbc` 传染给所有 agent 使用方）。把仓储契约抽成**一份共享抽象测试基类**，让内存实现与 JDBC 实现跑同一套断言，从机制上保证语义一致。JDBC 实现通过 `JdbcDialect` 分派方言（DDL 类型 + upsert 语句）。**H2 内存库是契约测试的主路径**（无需 Docker，必执行），MySQL/PostgreSQL 容器只做跨库补充验证。

**Tech Stack:** Java 17、`spring-jdbc`（`JdbcTemplate`）、H2 / MySQL / PostgreSQL、Testcontainers、JUnit 5 + AssertJ + Mockito、Maven（2.0.x 用 4.0.0-rc-6）。

**Spec:** `docs/superpowers/specs/2026-09-11-agent-dispatch-repository-jdbc-design.md`

## Global Constraints

- **Java 17**；两条线均 `<java.version>17</java.version>`。
- **构建**：`feature/2.0.x` 用 `/Users/wandl/tools/apache-maven-4.0.0-rc-6/bin/mvn`；`feature/1.0.x` 用 Maven 3。
- **JAVA_HOME 必须为 JDK 17**：`/Users/wandl/Library/Java/JavaVirtualMachines/corretto-17.0.20.1/Contents/Home`。
- **测试类必须以 `*Test` 结尾**：本仓 surefire 只收 `*Test`，且**未配置 failsafe**。叫 `*IT` 会静默不运行（骗人绿灯）。
- **`-pl` 解析不到新模块**：新增模块后必须同时在 `ddd4j-ai-extensions/pom.xml` 的 `<subprojects>` 与 `ddd4j-ai-bom/pom.xml` 的 `<dependencyManagement>` 登记。改动上游扩展后再测下游，一律加 `-am`（否则对着 `~/.m2` 旧 jar 跑，结论无效）。
- **磁盘**：本机曾因 97% 导致 Testcontainers `ContainerLaunchException`（现已 84%）。容器测试失败**先查磁盘**。
- **禁止 `git add -A` / `git add .`**：只显式 stage 本任务列出的路径。
- **`thenReturn` 泛型需类型见证**：如 `Mono.<Msg>just(...)`。
- **Mockito mock 不执行接口 default 方法**：mock 带 default 方法的接口时需显式 stub 下游会调用的方法。
- 所有测试离线可跑（H2 主路径不依赖 Docker；Docker 缺失时容器用例按既有 `disabledWithoutDocker` 语义跳过）。

---

## File Structure

| 文件 | 职责 | 动作 |
|------|------|------|
| `ddd4j-ai-extension-agent/src/test/java/io/ddd4j/ai/extension/agent/dispatch/AgentDispatchTaskRepositoryContract.java` | 共享契约测试基类（抽象） | 新增 |
| `ddd4j-ai-extension-agent/src/test/java/io/ddd4j/ai/extension/agent/dispatch/InMemoryAgentDispatchTaskRepositoryTest.java` | 内存实现的契约测试 | 新增 |
| `ddd4j-ai-extension-agent/pom.xml` | 加 `maven-jar-plugin` test-jar goal（供跨模块复用契约基类） | 改 |
| `ddd4j-ai-extensions/ddd4j-ai-extension-agent-store-jdbc/pom.xml` | 新模块 pom | 新增 |
| `.../agent-store-jdbc/src/main/java/io/ddd4j/ai/extension/agent/store/jdbc/JdbcDialect.java` | 方言：建表 / 建索引 / upsert SQL | 新增 |
| `.../store/jdbc/JdbcAgentDispatchTaskRepository.java` | `JdbcTemplate` 实现 4 个端口方法 | 新增 |
| `.../store/jdbc/autoconfigure/JdbcDispatchRepositoryAutoConfiguration.java` | 条件装配 + 按需建表 | 新增 |
| `.../store/jdbc/package-info.java` | 包文档 | 新增 |
| `.../agent-store-jdbc/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | 注册自动装配（**漏了装配静默失效**） | 新增 |
| `ddd4j-ai-extensions/pom.xml` | 登记 `<subproject>` | 改 |
| `ddd4j-ai-bom/pom.xml` | 登记 `dependencyManagement` | 改 |
| `.../agent-store-jdbc/src/test/java/.../JdbcAgentDispatchTaskRepositoryH2Test.java` | H2 契约测试（主路径，无需 Docker） | 新增 |
| `.../agent-store-jdbc/src/test/java/.../JdbcAgentDispatchTaskRepositoryMySqlTest.java` | MySQL 容器契约测试 | 新增 |
| `.../agent-store-jdbc/src/test/java/.../JdbcAgentDispatchTaskRepositoryPostgresTest.java` | PostgreSQL 容器契约测试 | 新增 |
| `.../agent-store-jdbc/src/test/java/.../JdbcDialectTest.java` | 方言 SQL 分派 | 新增 |
| `.../agent-store-jdbc/src/test/java/.../JdbcDispatchRepositoryAutoConfigurationTest.java` | 装配分支 | 新增 |

**关键设计说明（供实施者理解）：**
- 契约基类用"模板方法"模式：`protected abstract AgentDispatchTaskRepository newRepository()`，断言写在基类的 `@Test` 方法里；子类只提供实现实例。这样"三个实现跑同一套断言"是结构保证的，而非靠人工同步。
- **`save` 语义定为 upsert（按 id 幂等写）**：与既有 `InMemoryAgentDispatchTaskRepository` 的 `store.put(...)` 行为一致。JDBC 侧由 `JdbcDialect` 提供各方言 upsert 语句。`dispatchAll` 的重试路径依赖这一点。
- **`findByPlanId` 必须按 `createdAt` 升序**：与内存实现的显式 `sort(comparing createdAt)` 一致，契约测试会锚定。

---

## Task 1: 共享契约基类 + 内存实现契约测试

**Files:**
- Create: `ddd4j-ai-extension-agent/src/test/java/io/ddd4j/ai/extension/agent/dispatch/AgentDispatchTaskRepositoryContract.java`
- Create: `ddd4j-ai-extension-agent/src/test/java/io/ddd4j/ai/extension/agent/dispatch/InMemoryAgentDispatchTaskRepositoryTest.java`
- Modify: `ddd4j-ai-extension-agent/pom.xml`（加 test-jar）

**Interfaces:**
- Consumes: `AgentDispatchTaskRepository`（save / findById / findByPlanId / update）、`AgentDispatchTask`（record，6 字段；常量 `PENDING`/`RUNNING`/`DONE`/`FAILED`）
- Produces: `AgentDispatchTaskRepositoryContract`（抽象基类，`protected abstract AgentDispatchTaskRepository newRepository()`），供 Task 3/4 的 JDBC 测试继承

- [ ] **Step 1: 加 test-jar 发布配置**

在 `ddd4j-ai-extension-agent/pom.xml` 的 `<dependencies>` **之后**（`</project>` 之前）加：

```xml
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <executions>
                    <execution>
                        <goals>
                            <goal>test-jar</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
```

- [ ] **Step 2: 写契约基类**

```java
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
 * 从而以结构保证"内存实现与各持久化实现语义一致"，而非靠人工同步。
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
```

- [ ] **Step 3: 写内存实现的契约测试**

```java
package io.ddd4j.ai.extension.agent.dispatch;

/**
 * {@link InMemoryAgentDispatchTaskRepository} 契约测试：与 JDBC 实现跑同一套断言。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class InMemoryAgentDispatchTaskRepositoryTest extends AgentDispatchTaskRepositoryContract {

    @Override
    protected AgentDispatchTaskRepository newRepository() {
        return new InMemoryAgentDispatchTaskRepository();
    }
}
```

- [ ] **Step 4: 运行确认通过**

```bash
export JAVA_HOME=/Users/wandl/Library/Java/JavaVirtualMachines/corretto-17.0.20.1/Contents/Home
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
/Users/wandl/tools/apache-maven-4.0.0-rc-6/bin/mvn \
  -pl ddd4j-ai-extensions/ddd4j-ai-extension-agent \
  -Dtest=InMemoryAgentDispatchTaskRepositoryTest -Dsurefire.failIfNoSpecifiedTests=false \
  -Denforcer.skip=true -DskipITs test
```

Expected: `Tests run: 7, Failures: 0, Errors: 0`。

**若失败**：说明既有内存实现不满足契约（尤其 `createdAt` 升序或 upsert）。**不要改契约去迁就**——先判断是契约定错了还是实现有缺陷，把结论写进提交信息。

- [ ] **Step 5: 提交**

```bash
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
git add \
  ddd4j-ai-extensions/ddd4j-ai-extension-agent/pom.xml \
  ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/test/java/io/ddd4j/ai/extension/agent/dispatch/AgentDispatchTaskRepositoryContract.java \
  ddd4j-ai-extensions/ddd4j-ai-extension-agent/src/test/java/io/ddd4j/ai/extension/agent/dispatch/InMemoryAgentDispatchTaskRepositoryTest.java
git commit -m "test(agent): add a shared AgentDispatchTaskRepository contract base

The contract is an abstract test base so the in-memory implementation and every
future persistent implementation are held to one set of assertions structurally,
rather than by manual sync. Publishes the agent extension's test classes as a
test-jar so the JDBC module can reuse the base.

Pins two semantics that matter: findByPlanId orders by createdAt ascending, and
save is an idempotent upsert by id."
```

---

## Task 2: 新模块骨架（可构建）

**Files:**
- Create: `ddd4j-ai-extensions/ddd4j-ai-extension-agent-store-jdbc/pom.xml`
- Create: `.../src/main/java/io/ddd4j/ai/extension/agent/store/jdbc/package-info.java`
- Modify: `ddd4j-ai-extensions/pom.xml`
- Modify: `ddd4j-ai-bom/pom.xml`

**Interfaces:**
- Produces: 一个能被 `-pl` 定位、能编译的空模块 `ddd4j-ai-extension-agent-store-jdbc`

- [ ] **Step 1: 写新模块 pom**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.1.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.1.0 https://maven.apache.org/xsd/maven-4.1.0.xsd">
    <modelVersion>4.1.0</modelVersion>
    <parent>
        <groupId>io.ddd4j.ai</groupId>
        <artifactId>ddd4j-ai-extensions</artifactId>
        <version>${revision}</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>ddd4j-ai-extension-agent-store-jdbc</artifactId>
    <description>
        智能体派发任务仓储的 JDBC 实现：一次覆盖 PostgreSQL / MySQL / H2，
        消除"进程重启即丢派发任务"的缺陷。可选构件，仅需持久化的业务引入。
    </description>

    <dependencies>
        <dependency>
            <groupId>io.ddd4j.ai</groupId>
            <artifactId>ddd4j-ai-extension-agent</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-jdbc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <!-- 契约基类来自 agent 扩展的 test-jar -->
        <dependency>
            <groupId>io.ddd4j.ai</groupId>
            <artifactId>ddd4j-ai-extension-agent</artifactId>
            <version>${revision}</version>
            <type>test-jar</type>
            <scope>test</scope>
        </dependency>
        <!-- H2：契约测试主路径，无需 Docker -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
        <!-- 跨库补充验证：MySQL -->
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>mysql</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>test</scope>
        </dependency>
        <!-- 跨库补充验证：PostgreSQL -->
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

</project>
```

- [ ] **Step 2: 写 package-info**

```java
/**
 * 智能体派发任务仓储的 JDBC 实现：以 {@code DataSource} 为接入点，
 * 一次覆盖 PostgreSQL / MySQL / H2。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
package io.ddd4j.ai.extension.agent.store.jdbc;
```

- [ ] **Step 3: 登记到聚合器与 BOM**

`ddd4j-ai-extensions/pom.xml` 的 `<subprojects>` 内追加（与既有条目同缩进）：

```xml
        <subproject>ddd4j-ai-extension-agent-store-jdbc</subproject>
```

`ddd4j-ai-bom/pom.xml` 的 `<dependencyManagement><dependencies>` 内追加：

```xml
            <dependency>
                <groupId>io.ddd4j.ai</groupId>
                <artifactId>ddd4j-ai-extension-agent-store-jdbc</artifactId>
                <version>${revision}</version>
            </dependency>
```

- [ ] **Step 4: 构建确认模块可被定位与编译**

```bash
export JAVA_HOME=/Users/wandl/Library/Java/JavaVirtualMachines/corretto-17.0.20.1/Contents/Home
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
/Users/wandl/tools/apache-maven-4.0.0-rc-6/bin/mvn \
  -pl ddd4j-ai-extensions/ddd4j-ai-extension-agent-store-jdbc -am \
  -Denforcer.skip=true -DskipITs -DskipTests install
```

Expected: Reactor 含 `ddd4j-ai-extension-agent-store-jdbc ... SUCCESS`；`BUILD SUCCESS`。

**若报 `Could not find the selected project in the reactor`**：说明 Step 3 的聚合器登记漏了或写错缩进。

- [ ] **Step 5: 提交**

```bash
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
git add \
  ddd4j-ai-extensions/pom.xml \
  ddd4j-ai-bom/pom.xml \
  ddd4j-ai-extensions/ddd4j-ai-extension-agent-store-jdbc/pom.xml \
  ddd4j-ai-extensions/ddd4j-ai-extension-agent-store-jdbc/src/main/java/io/ddd4j/ai/extension/agent/store/jdbc/package-info.java
git commit -m "feat(agent-store-jdbc): scaffold the JDBC dispatch repository module

A separate extension module so spring-jdbc reaches only the applications that
opt into persistence, instead of every consumer of the agent extension.
Registered in the extensions aggregator and the BOM so -pl can resolve it."
```

---

## Task 3: JdbcDialect + 仓储实现 + 装配 + H2 契约测试

**Files:**
- Create: `.../store/jdbc/JdbcDialect.java`
- Create: `.../store/jdbc/JdbcAgentDispatchTaskRepository.java`
- Create: `.../store/jdbc/autoconfigure/JdbcDispatchRepositoryAutoConfiguration.java`
- Create: `.../src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Create: `.../src/test/java/.../JdbcDialectTest.java`
- Create: `.../src/test/java/.../JdbcAgentDispatchTaskRepositoryH2Test.java`

**Interfaces:**
- Consumes: Task 1 的 `AgentDispatchTaskRepositoryContract`（test-jar）；`AgentDispatchTask` / `AgentDispatchTaskRepository` / `AgentDispatchTask.PENDING|RUNNING|DONE|FAILED`
- Produces: `JdbcDialect.from(DataSource) → JdbcDialect`（方言探测）；`JdbcDialect.createTableSql()/createIndexSql()/upsertSql()`；`JdbcAgentDispatchTaskRepository(DataSource, JdbcDialect, boolean autoDdl)`

- [ ] **Step 1: 写方言测试（失败）**

```java
package io.ddd4j.ai.extension.agent.store.jdbc;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcDialectTest {

    @Test
    void mysqlUsesClobAndOnDuplicateKey() {
        JdbcDialect dialect = JdbcDialect.forProductName("MySQL");
        assertThat(dialect.createTableSql()).contains("CLOB");
        assertThat(dialect.upsertSql()).contains("ON DUPLICATE KEY UPDATE");
    }

    @Test
    void postgresUsesTextAndOnConflict() {
        JdbcDialect dialect = JdbcDialect.forProductName("PostgreSQL");
        assertThat(dialect.createTableSql()).contains("TEXT").doesNotContain("CLOB");
        assertThat(dialect.upsertSql()).contains("ON CONFLICT").contains("DO UPDATE");
    }

    @Test
    void h2UsesTextAndMerge() {
        JdbcDialect dialect = JdbcDialect.forProductName("H2");
        assertThat(dialect.createTableSql()).contains("TEXT").doesNotContain("CLOB");
        assertThat(dialect.upsertSql()).contains("MERGE INTO");
    }

    @Test
    void unknownProductFallsBackToH2Compatible() {
        assertThat(JdbcDialect.forProductName("SomeUnknownDb").upsertSql()).contains("MERGE INTO");
    }
}
```

- [ ] **Step 2: 运行确认失败**

```bash
export JAVA_HOME=/Users/wandl/Library/Java/JavaVirtualMachines/corretto-17.0.20.1/Contents/Home
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
/Users/wandl/tools/apache-maven-4.0.0-rc-6/bin/mvn \
  -pl ddd4j-ai-extensions/ddd4j-ai-extension-agent-store-jdbc \
  -Dtest=JdbcDialectTest -Dsurefire.failIfNoSpecifiedTests=false \
  -Denforcer.skip=true -DskipITs test
```

Expected: 编译失败，`找不到符号: 类 JdbcDialect`。

- [ ] **Step 3: 实现 JdbcDialect**

```java
package io.ddd4j.ai.extension.agent.store.jdbc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * JDBC 方言：承载建表 / 建索引 / upsert 三处不可避免的方言差异。
 *
 * <p>探测失败或未知产品名时回落到 H2 兼容写法（`MERGE INTO`），
 * 因为它是 SQL 标准里最接近"按主键 upsert"的表述。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public enum JdbcDialect {

    MYSQL,
    POSTGRESQL,
    H2;

    /** 大文本列类型：MySQL 用 CLOB，其余用 TEXT。 */
    public String textType() {
        return this == MYSQL ? "CLOB" : "TEXT";
    }

    public String createTableSql() {
        return "CREATE TABLE IF NOT EXISTS ddd4j_ai_agent_dispatch_task ("
                + "id VARCHAR(64) NOT NULL, "
                + "plan_id VARCHAR(64) NOT NULL, "
                + "instruction " + textType() + " NOT NULL, "
                + "status VARCHAR(16) NOT NULL, "
                + "result " + textType() + ", "
                + "created_at TIMESTAMP NOT NULL, "
                + "PRIMARY KEY (id))";
    }

    public String createIndexSql() {
        return "CREATE INDEX IF NOT EXISTS idx_ddd4j_ai_dispatch_plan "
                + "ON ddd4j_ai_agent_dispatch_task (plan_id)";
    }

    public String upsertSql() {
        String columns = "(id, plan_id, instruction, status, result, created_at)";
        String values = "(?, ?, ?, ?, ?, ?)";
        return switch (this) {
            case MYSQL -> "INSERT INTO ddd4j_ai_agent_dispatch_task " + columns + " VALUES " + values
                    + " ON DUPLICATE KEY UPDATE plan_id=VALUES(plan_id), instruction=VALUES(instruction),"
                    + " status=VALUES(status), result=VALUES(result), created_at=VALUES(created_at)";
            case POSTGRESQL -> "INSERT INTO ddd4j_ai_agent_dispatch_task " + columns + " VALUES " + values
                    + " ON CONFLICT (id) DO UPDATE SET plan_id=EXCLUDED.plan_id,"
                    + " instruction=EXCLUDED.instruction, status=EXCLUDED.status,"
                    + " result=EXCLUDED.result, created_at=EXCLUDED.created_at";
            case H2 -> "MERGE INTO ddd4j_ai_agent_dispatch_task " + columns + " KEY(id) VALUES " + values;
        };
    }

    /** 按产品名选择方言；未知回落 H2 兼容写法。 */
    public static JdbcDialect forProductName(String productName) {
        if (productName == null) {
            return H2;
        }
        String normalized = productName.toLowerCase();
        if (normalized.contains("mysql") || normalized.contains("mariadb")) {
            return MYSQL;
        }
        if (normalized.contains("postgres")) {
            return POSTGRESQL;
        }
        return H2;
    }

    /** 从连接元数据探测方言。 */
    public static JdbcDialect from(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            return forProductName(connection.getMetaData().getDatabaseProductName());
        } catch (SQLException e) {
            throw new IllegalStateException("无法探测数据库方言: " + e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 4: 运行确认方言测试通过**

同 Step 2 命令。Expected: `Tests run: 4, Failures: 0, Errors: 0`。

- [ ] **Step 5: 写 H2 契约测试（失败）**

```java
package io.ddd4j.ai.extension.agent.store.jdbc;

import io.ddd4j.ai.extension.agent.dispatch.AgentDispatchTaskRepository;
import io.ddd4j.ai.extension.agent.dispatch.AgentDispatchTaskRepositoryContract;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * H2 内存库的契约测试——**主路径，无需 Docker**，因此在任何环境都真实执行。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class JdbcAgentDispatchTaskRepositoryH2Test extends AgentDispatchTaskRepositoryContract {

    private static DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .generateUniqueName(true)
                .build();
    }

    @Override
    protected AgentDispatchTaskRepository newRepository() {
        return new JdbcAgentDispatchTaskRepository(dataSource(), JdbcDialect.H2, true);
    }

    @Test
    void autoDdlFalse_doesNotCreateTable() {
        JdbcAgentDispatchTaskRepository repository =
                new JdbcAgentDispatchTaskRepository(dataSource(), JdbcDialect.H2, false);
        // 未建表时任何查询都应失败，而不是静默返回空——静默会掩盖配置错误
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> repository.findByPlanId("p1"));
    }
}
```

- [ ] **Step 6: 实现仓储 + 装配**

`JdbcAgentDispatchTaskRepository.java`：

```java
package io.ddd4j.ai.extension.agent.store.jdbc;

import io.ddd4j.ai.extension.agent.dispatch.AgentDispatchTask;
import io.ddd4j.ai.extension.agent.dispatch.AgentDispatchTaskRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link AgentDispatchTaskRepository} 的 JDBC 实现：一次覆盖 PostgreSQL / MySQL / H2。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class JdbcAgentDispatchTaskRepository implements AgentDispatchTaskRepository {

    private static final RowMapper<AgentDispatchTask> ROW_MAPPER = (rs, rowNum) -> new AgentDispatchTask(
            rs.getString("id"),
            rs.getString("plan_id"),
            rs.getString("instruction"),
            rs.getString("status"),
            rs.getString("result"),
            rs.getTimestamp("created_at").toInstant());

    private final JdbcTemplate jdbcTemplate;
    private final JdbcDialect dialect;

    public JdbcAgentDispatchTaskRepository(DataSource dataSource, JdbcDialect dialect, boolean autoDdl) {
        Objects.requireNonNull(dataSource, "dataSource");
        this.dialect = Objects.requireNonNull(dialect, "dialect");
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        if (autoDdl) {
            this.jdbcTemplate.execute(dialect.createTableSql());
            this.jdbcTemplate.execute(dialect.createIndexSql());
        }
    }

    @Override
    public void save(AgentDispatchTask task) {
        jdbcTemplate.update(dialect.upsertSql(),
                task.id(), task.planId(), task.instruction(), task.status(), task.result(),
                Timestamp.from(task.createdAt()));
    }

    @Override
    public Optional<AgentDispatchTask> findById(String id) {
        List<AgentDispatchTask> found =
                jdbcTemplate.query("SELECT * FROM ddd4j_ai_agent_dispatch_task WHERE id = ?", ROW_MAPPER, id);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
    }

    @Override
    public List<AgentDispatchTask> findByPlanId(String planId) {
        return jdbcTemplate.query(
                "SELECT * FROM ddd4j_ai_agent_dispatch_task WHERE plan_id = ? ORDER BY created_at ASC",
                ROW_MAPPER, planId);
    }

    @Override
    public void update(AgentDispatchTask task) {
        // 与 save 同为按 id 幂等写：update 语义上要求已存在，但 upsert 实现
        // 使重试/乱序调用都不会丢数据，且与 InMemory 实现（put）行为一致。
        save(task);
    }
}
```

`JdbcDispatchRepositoryAutoConfiguration.java`：

```java
package io.ddd4j.ai.extension.agent.store.jdbc.autoconfigure;

import io.ddd4j.ai.extension.agent.dispatch.AgentDispatchTaskRepository;
import io.ddd4j.ai.extension.agent.store.jdbc.JdbcAgentDispatchTaskRepository;
import io.ddd4j.ai.extension.agent.store.jdbc.JdbcDialect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * JDBC 派发任务仓储自动装配：仅在显式开启且存在 DataSource 时生效。
 * 不配置则维持 agent 扩展既有的内存实现，既有用户零影响。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@AutoConfiguration
@ConditionalOnClass(JdbcTemplate.class)
@ConditionalOnProperty(name = "ddd4j.ai.agent.dispatch.repository", havingValue = "jdbc")
@ConditionalOnBean(DataSource.class)
public class JdbcDispatchRepositoryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AgentDispatchTaskRepository.class)
    public AgentDispatchTaskRepository jdbcAgentDispatchTaskRepository(
            DataSource dataSource,
            org.springframework.core.env.Environment environment) {
        boolean autoDdl = environment.getProperty(
                "ddd4j.ai.agent.dispatch.auto-ddl", Boolean.class, Boolean.TRUE);
        return new JdbcAgentDispatchTaskRepository(dataSource, JdbcDialect.from(dataSource), autoDdl);
    }
}
```

`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`：

```
io.ddd4j.ai.extension.agent.store.jdbc.autoconfigure.JdbcDispatchRepositoryAutoConfiguration
```

- [ ] **Step 7: 运行 H2 契约测试确认通过**

```bash
export JAVA_HOME=/Users/wandl/Library/Java/JavaVirtualMachines/corretto-17.0.20.1/Contents/Home
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
/Users/wandl/tools/apache-maven-4.0.0-rc-6/bin/mvn \
  -pl ddd4j-ai-extensions/ddd4j-ai-extension-agent-store-jdbc -am \
  -Dtest=JdbcAgentDispatchTaskRepositoryH2Test -Dsurefire.failIfNoSpecifiedTests=false \
  -Denforcer.skip=true -DskipITs test
```

Expected: `Tests run: 8, Failures: 0, Errors: 0`（契约 7 + autoDdlFalse 1）。

- [ ] **Step 8: 提交**

```bash
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
git add ddd4j-ai-extensions/ddd4j-ai-extension-agent-store-jdbc/src
git commit -m "feat(agent-store-jdbc): add JDBC dispatch repository with dialect dispatch

JdbcDialect carries the three genuinely dialect-specific statements (text column
type, index creation, primary-key upsert) so the repository stays dialect-free.
H2 is the contract-test main path because it needs no Docker and therefore
always executes, instead of silently skipping on machines without it.

Auto-configuration is opt-in via ddd4j.ai.agent.dispatch.repository=jdbc plus a
present DataSource, so existing users are untouched."
```

---

## Task 4: 装配测试 + MySQL/PostgreSQL 容器契约测试

**Files:**
- Create: `.../src/test/java/.../JdbcDispatchRepositoryAutoConfigurationTest.java`
- Create: `.../src/test/java/.../JdbcAgentDispatchTaskRepositoryMySqlTest.java`
- Create: `.../src/test/java/.../JdbcAgentDispatchTaskRepositoryPostgresTest.java`

**Interfaces:**
- Consumes: Task 3 的 `JdbcAgentDispatchTaskRepository` / `JdbcDialect` / `JdbcDispatchRepositoryAutoConfiguration`；Task 1 的契约基类
- Produces: 跨库验证证据（MySQL + PostgreSQL 与 H2 语义一致）

- [ ] **Step 1: 写装配测试**

```java
package io.ddd4j.ai.extension.agent.store.jdbc.autoconfigure;

import io.ddd4j.ai.extension.agent.dispatch.AgentDispatchTaskRepository;
import io.ddd4j.ai.extension.agent.dispatch.InMemoryAgentDispatchTaskRepository;
import io.ddd4j.ai.extension.agent.store.jdbc.JdbcAgentDispatchTaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcDispatchRepositoryAutoConfigurationTest {

    @Configuration
    static class DataSourceConfig {
        @Bean
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder()
                    .setType(EmbeddedDatabaseType.H2).generateUniqueName(true).build();
        }
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JdbcDispatchRepositoryAutoConfiguration.class));

    @Test
    void jdbcPropertyWithDataSource_registersJdbcRepository() {
        runner.withUserConfiguration(DataSourceConfig.class)
                .withPropertyValues("ddd4j.ai.agent.dispatch.repository=jdbc")
                .run(context -> assertThat(context).hasSingleBean(JdbcAgentDispatchTaskRepository.class));
    }

    @Test
    void withoutProperty_backsOff() {
        runner.withUserConfiguration(DataSourceConfig.class)
                .run(context -> assertThat(context)
                        .doesNotHaveBean(AgentDispatchTaskRepository.class));
    }

    @Test
    void jdbcPropertyWithoutDataSource_backsOff() {
        runner.withPropertyValues("ddd4j.ai.agent.dispatch.repository=jdbc")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(AgentDispatchTaskRepository.class));
    }

    @Test
    void autoDdlFalse_bootsWithoutCreatingTable() {
        runner.withUserConfiguration(DataSourceConfig.class)
                .withPropertyValues(
                        "ddd4j.ai.agent.dispatch.repository=jdbc",
                        "ddd4j.ai.agent.dispatch.auto-ddl=false")
                .run(context -> assertThat(context).hasSingleBean(JdbcAgentDispatchTaskRepository.class));
    }
}
```

- [ ] **Step 2: 运行装配测试确认通过**

```bash
export JAVA_HOME=/Users/wandl/Library/Java/JavaVirtualMachines/corretto-17.0.20.1/Contents/Home
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
/Users/wandl/tools/apache-maven-4.0.0-rc-6/bin/mvn \
  -pl ddd4j-ai-extensions/ddd4j-ai-extension-agent-store-jdbc -am \
  -Dtest=JdbcDispatchRepositoryAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false \
  -Denforcer.skip=true -DskipITs test
```

Expected: `Tests run: 4, Failures: 0, Errors: 0`。

- [ ] **Step 3: 写容器多库契约测试（两个顶层类，避免 `@Nested` 生命周期坑）**

刻意不用 `@Testcontainers` + `@Nested` + 非静态 `@Container` 的组合——那是边界用法，容器生命周期与 JUnit 嵌套类交互有已知陷阱。改用最稳的 canonical 形态：**每个库一个顶层类，`static final @Container`**。

`JdbcAgentDispatchTaskRepositoryMySqlTest.java`：

```java
package io.ddd4j.ai.extension.agent.store.jdbc;

import io.ddd4j.ai.extension.agent.dispatch.AgentDispatchTaskRepository;
import io.ddd4j.ai.extension.agent.dispatch.AgentDispatchTaskRepositoryContract;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * MySQL 容器契约测试：与 H2 主路径跑同一套断言，验证跨库语义一致。
 *
 * <p>类名必须以 {@code Test} 结尾——本仓 surefire 无 failsafe，叫 {@code *IT} 会静默不运行。
 * 无 Docker 时按 Testcontainers 语义跳过。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Testcontainers
class JdbcAgentDispatchTaskRepositoryMySqlTest extends AgentDispatchTaskRepositoryContract {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    @Override
    protected AgentDispatchTaskRepository newRepository() {
        return new JdbcAgentDispatchTaskRepository(
                new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword()),
                JdbcDialect.MYSQL, true);
    }
}
```

`JdbcAgentDispatchTaskRepositoryPostgresTest.java`：

```java
package io.ddd4j.ai.extension.agent.store.jdbc;

import io.ddd4j.ai.extension.agent.dispatch.AgentDispatchTaskRepository;
import io.ddd4j.ai.extension.agent.dispatch.AgentDispatchTaskRepositoryContract;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * PostgreSQL 容器契约测试：与 H2 主路径跑同一套断言，验证跨库语义一致。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Testcontainers
class JdbcAgentDispatchTaskRepositoryPostgresTest extends AgentDispatchTaskRepositoryContract {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Override
    protected AgentDispatchTaskRepository newRepository() {
        return new JdbcAgentDispatchTaskRepository(
                new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()),
                JdbcDialect.POSTGRESQL, true);
    }
}
```

- [ ] **Step 4: 运行容器契约测试**

```bash
export JAVA_HOME=/Users/wandl/Library/Java/JavaVirtualMachines/corretto-17.0.20.1/Contents/Home
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
df -h /System/Volumes/Data | tail -1   # 磁盘需充裕，否则容器起不来
/Users/wandl/tools/apache-maven-4.0.0-rc-6/bin/mvn \
  -pl ddd4j-ai-extensions/ddd4j-ai-extension-agent-store-jdbc -am \
  -Dtest='JdbcAgentDispatchTaskRepositoryMySqlTest,JdbcAgentDispatchTaskRepositoryPostgresTest' \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Denforcer.skip=true -DskipITs test
```

Expected: `Tests run: 14, Failures: 0, Errors: 0`（MySQL 7 + PostgreSQL 7）。

**若容器启动失败**：先查磁盘（`ContainerLaunchException` 的已知成因），再查镜像是否已拉取。

- [ ] **Step 5: 运行模块全量测试**

```bash
export JAVA_HOME=/Users/wandl/Library/Java/JavaVirtualMachines/corretto-17.0.20.1/Contents/Home
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
/Users/wandl/tools/apache-maven-4.0.0-rc-6/bin/mvn \
  -pl ddd4j-ai-extensions/ddd4j-ai-extension-agent-store-jdbc -am \
  -Denforcer.skip=true -DskipITs test
```

Expected: `BUILD SUCCESS`，含 H2 契约 + 容器契约 + 方言 + 装配全部用例；上游 agent 模块既有测试零回归。

- [ ] **Step 6: 提交**

```bash
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
git add ddd4j-ai-extensions/ddd4j-ai-extension-agent-store-jdbc/src/test
git commit -m "test(agent-store-jdbc): verify the JDBC repository on H2, MySQL and PostgreSQL

Both container-backed contracts run the same abstract assertions as the H2 main
path and the in-memory implementation, so cross-database semantic drift shows up
as a test failure rather than as a production surprise. Auto-configuration is
covered for all four branches (property present/absent, DataSource present/absent,
auto-ddl off)."
```

---

## Task 5: 推送 + 执行记录

- [ ] **Step 1: 推送 2.0.x**

```bash
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
git fetch github feature/2.0.x && git fetch origin feature/2.0.x
git log --oneline -1 github/feature/2.0.x   # 确认无并行会话分叉
git push github feature/2.0.x && git push origin feature/2.0.x
```

**不加 `-f`。** 若发现远端有本会话之外的提交，先停下来报告。

- [ ] **Step 2: 执行记录回写 spec**

在 spec（`docs/superpowers/specs/2026-09-11-agent-dispatch-repository-jdbc-design.md`）末尾追加 `## 11. 实施记录（2026-09-11）`，内容包含：各 Task 提交号与测试数、H2/MySQL/PG 三实现契约通过的证据、实施中发现的新事实（若有）、以及 1.0.x 阻塞状态。提交并推送。

---

## 验收清单

- [ ] `AgentDispatchTaskRepositoryContract` 存在且为抽象基类；内存实现继承并全部通过。
- [ ] 新模块 `ddd4j-ai-extension-agent-store-jdbc` 已登记进 `ddd4j-ai-extensions` 聚合器与 `ddd4j-ai-bom`。
- [ ] `JdbcDialect` 覆盖 MySQL / PostgreSQL / H2，未知产品名回落 H2 兼容写法（4/4 测试）。
- [ ] `JdbcAgentDispatchTaskRepository` 通过 H2 契约测试（无需 Docker，必执行）。
- [ ] 同一套契约断言在 **内存 / H2 / MySQL / PostgreSQL** 四个实现上全部通过。
- [ ] 装配测试覆盖 4 个分支：属性开+DataSource有 / 属性缺 / DataSource缺 / `auto-ddl=false`。
- [ ] 不设 `ddd4j.ai.agent.dispatch.repository` 时行为与今天一致（agent 模块既有测试零回归）。
- [ ] `AutoConfiguration.imports` 已创建且内容正确。
- [ ] 所有测试类以 `*Test` 结尾（无 `*IT`，避免静默不跑）。
- [ ] 未把 RabbitMQ / RocketMQ 塞进仓储接口（各自需 `AgentDispatchTaskQueue` 新端口，另项）。
- [ ] 两线同步（1.0.x 受阻塞时先交付 2.0.x 并如实标注）。

## 明确不在本计划范围

- Redis / MongoDB 后端（按同一"六件套"模板各自独立模块，后续批次）
- RabbitMQ / RocketMQ 的 `AgentDispatchTaskQueue` 执行通道端口（独立一项）
- Micrometer 埋点（第 5 项）
- 修 1.0.x 的既有 POM 缺陷（需单独授权）
