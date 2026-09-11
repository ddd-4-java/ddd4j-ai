# Agent 派发任务仓储 JDBC 持久化设计（ddd4j-ai-extension-agent-store-jdbc）

- 日期：2026-09-11
- 作者：PartMe.AI
- 状态：待实施（结构已定：独立扩展模块 + 本批只做 JDBC）
- 范围：新增 `ddd4j-ai-extensions/ddd4j-ai-extension-agent-store-jdbc` 模块
- 分支：`feature/2.0.x`（实施与验证）+ `feature/1.0.x`（同补丁移植，前提见 §10）
- 关联：本文件是「ddd4j-ai 优化改造」6 项序列中的**第 3 项**；第 1 项见 `2026-09-11-agent-fine-grained-event-stream-design.md`，第 2 项见 `2026-09-11-agent-blocking-call-remediation-design.md`

---

## 1. 背景

`AgentDispatchTaskRepository` 是 `AgentPlanOrchestrator` 的派发任务状态端口，当前**只有** `InMemoryAgentDispatchTaskRepository`（`ConcurrentHashMap`）一个实现。

`AgentPlanOrchestrator` 的 `submitPlan → dispatchAll → mergeResults` 是**长流程**：一个计划下多个子任务并行执行，可能分钟级。一旦进程重启：

- 所有 `PENDING` / `RUNNING` 任务**静默消失**
- 无法恢复、无法重试、无审计痕迹
- `mergeResults` 因任务不存在而失败

接口本身早已设计完备（`save` / `findById` / `findByPlanId` / `update`），本项补的是**生产可用的实现**。

## 2. 关键事实（实测）

**F1. agent 扩展没有任何 JDBC 抽象可用**
`mvn dependency:tree` 实测，`ddd4j-ai-extension-agent` 的依赖里**没有** `spring-jdbc` / `spring-tx` / `JdbcTemplate` / MyBatis / Spring Data / Redis 客户端。仅有：
- `io.agentscope:agentscope-extensions-mysql:2.0.2`（compile）
- `org.testcontainers:mysql:1.20.6`（**test** scope）
- `com.mysql:mysql-connector-j:9.7.0`（**test** scope）

**F2. `DataSource` 已是既有集成点**
`AgentAutoConfiguration.agentStateStore` 在 `ddd4j.ai.agent.state-store=mysql` 时用 `javax.sql.DataSource` 构造 `MysqlAgentStateStore`——说明"业务提供 DataSource"的模式在本扩展里已有先例，本项沿用。

**F3. Testcontainers MySQL 在本仓已验证可行**
`MysqlStateStoreWiringTest` 就是用 Testcontainers 起 `mysql:8.0` 做装配验证（磁盘充足时 2/2 通过；磁盘满时表现为 `ContainerLaunchException`）。

**F4. 接口语义是"状态存储"，不是队列**
```java
public interface AgentDispatchTaskRepository {
    void save(AgentDispatchTask task);
    Optional<AgentDispatchTask> findById(String id);
    List<AgentDispatchTask> findByPlanId(String planId);
    void update(AgentDispatchTask task);
}
```
`findByPlanId` 要求"按计划列清单"、`update` 要求"原地改状态"——这是**存储**语义。

**F5. `AgentDispatchTask` 是 record，字段扁平**
```java
public record AgentDispatchTask(String id, String planId, String instruction,
                                String status, String result, Instant createdAt)
```
6 个字段全部是 `String` / `Instant` → 单表 6 列即可承载，无需 JSON 序列化。

**F6. 模块聚合与 BOM 均为显式列举**
`ddd4j-ai-extensions/pom.xml` 用 `<subproject>` 列举子模块（Maven 4 语法）；`ddd4j-ai-bom/pom.xml` 用 `<dependencyManagement>` 列举全部扩展。**新增模块必须同时改这两处**，否则 `-pl` 与业务引用都会找不到。

## 3. 目标

- 提供 JDBC 实现的 `AgentDispatchTaskRepository`，**一次覆盖 PostgreSQL / MySQL / H2**（凡提供标准 `DataSource` 均可）。
- 保持 `AgentDispatchTaskRepository` 接口与 `AgentPlanOrchestrator` **零改动**。
- 默认行为不变：不配置即维持内存实现（既有用户无感知）。
- 建立可复用的"六件套"模板，后续 Redis / MongoDB 后端按同一模板复制。
- 表结构自动创建（可关）。

### 非目标

- **不在本批实现 Redis / MongoDB 后端**（结构已定：后续按同一模板各自独立模块）。
- **不为 RabbitMQ / RocketMQ 提供实现**。原因见 §4 决策 4：`findByPlanId` / `update` 是存储语义，消息中间件无法表达；MQ 契合的是"派发执行通道"，需要**另一个端口**（`AgentDispatchTaskQueue`），属独立一项。
- 不改 `AgentDispatchTaskRepository` 接口（不为其加 send/receive 之类）。
- 不引入连接池实现（HikariCP 等由业务侧的 `spring-boot-starter-jdbc` 提供，本模块只消费 `DataSource`）。
- 不做 Micrometer 埋点（第 5 项）。

## 4. 关键决策

### 决策 1：独立扩展模块，不并入 agent 扩展

新建 `ddd4j-ai-extension-agent-store-jdbc`。理由：

- 与项目既有惯例一致（14 个扩展"一能力一构件"，业务按需引入），对齐 `spring-ai-starter-vector-store-*` 的生态做法。
- **避免依赖传染**：并入 agent 扩展会让每个使用智能体的业务都拖上 `spring-jdbc`。独立模块则只有需要持久化的业务才引入。
- 为后续 Redis / MongoDB 后端提供对称的落点。

### 决策 2：用 `spring-jdbc` 的 `JdbcTemplate`，不手写裸 JDBC

- `JdbcTemplate` 负责资源关闭与 `DataAccessException` 异常转换——手写 `Connection`/`PreparedStatement` 的 try-with-resources 是真实的泄漏与正确性风险，收益（省一个依赖）与风险不相称。
- 本模块是 **opt-in** 构件，业务主动引入，多一个由 Boot BOM 正常管理版本的 `spring-jdbc` 是相称的（与"不给核心 agent 扩展加依赖"是两回事）。
- **替代路径**：若业务侧有强零依赖要求，可改为 `javax.sql.DataSource` + `java.sql.*` 手写（约多 40 行）。本设计与计划按 `JdbcTemplate` 展开；改用裸 JDBC 只影响实现类内部，端口与装配不变。

### 决策 3：默认内存实现不变，JDBC 为显式开关

`AgentAutoConfiguration` 维持现有内存实现的装配不变；本模块**另加**一条分支：

```java
@ConditionalOnProperty(name = "ddd4j.ai.agent.dispatch.repository", havingValue = "jdbc")
@ConditionalOnClass(JdbcTemplate.class)
@ConditionalOnBean(DataSource.class)
```

`ddd4j.ai.agent.dispatch.repository` 取值 `memory`（默认）/ `jdbc`。**不配置时行为与今天完全一致**，既有用户零影响。

### 决策 4：MQ 不是仓储，另立端口

`AgentDispatchTaskRepository` 的 `findByPlanId`（按计划列清单）与 `update`（原地改状态）**消息中间件做不到**：队列无法按 planId 枚举，也无法更新已发出的消息。

若强行实现，只能是"实现撒谎"（`findByPlanId` 返回空/抛异常）或"接口被污染"（加入 send/receive）。二者都不可接受。

MQ 真正契合的是**派发执行通道**（把任务投递给 worker 消费）。因此设计上区分两个协作端口：

| 关注点 | 端口 | 适配后端 |
|--------|------|---------|
| 任务**状态**（可查、可更新、可审计） | `AgentDispatchTaskRepository`（已存在） | JDBC / Redis / MongoDB |
| 任务**投递**（发出去让 worker 干） | `AgentDispatchTaskQueue`（**新增，属独立一项**） | RabbitMQ / RocketMQ |

本项只做前者。

### 决策 5：自动建表，默认开、可关

- `ddd4j.ai.agent.dispatch.auto-ddl`（默认 `true`）：装配时执行 `CREATE TABLE IF NOT EXISTS`。
- 置 `false` 则不自建，由业务接入自己的迁移工具（Flyway / Liquibase）。
- **该开关仅对 JDBC 系有效**：Redis / MongoDB 无表结构概念，后续后端会忽略它。

## 5. 表结构与实现方向

```sql
CREATE TABLE IF NOT EXISTS ddd4j_ai_agent_dispatch_task (
    id          VARCHAR(64)  NOT NULL,
    plan_id     VARCHAR(64)  NOT NULL,
    instruction CLOB         NOT NULL,
    status      VARCHAR(16)  NOT NULL,
    result      CLOB,
    created_at  TIMESTAMP    NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_ddd4j_ai_dispatch_plan ON ddd4j_ai_agent_dispatch_task (plan_id);
```

移植性说明：`CLOB` 与 `CREATE INDEX IF NOT EXISTS` 在 PostgreSQL 下不兼容（PG 用 `TEXT`，且不支持 `IF NOT EXISTS` 建索引）。实施时按方言分派：

- 通用/MySQL/H2：用上表
- PostgreSQL：`instruction TEXT` / `result TEXT`；索引用 `CREATE INDEX IF NOT EXISTS`（PG 9.5+ 支持）

**`findByPlanId` 必须按 `created_at` 升序返回**——与 `InMemoryAgentDispatchTaskRepository` 的既有排序语义一致（该实现显式 `sort by createdAt`），契约测试会锚定这一点。

## 6. 模块落点

| 文件 | 动作 | 说明 |
|------|------|------|
| `ddd4j-ai-extensions/ddd4j-ai-extension-agent-store-jdbc/pom.xml` | 新增 | parent = `ddd4j-ai-extensions`；依赖 `ddd4j-ai-extension-agent`、`spring-jdbc`、`spring-boot-autoconfigure`；test: `spring-boot-starter-test`、`testcontainers:mysql`、`mysql-connector-j`、`testcontainers:postgresql`、`postgresql` |
| `.../src/main/java/io/ddd4j/ai/extension/agent/store/jdbc/JdbcAgentDispatchTaskRepository.java` | 新增 | `implements AgentDispatchTaskRepository` |
| `.../src/main/java/io/ddd4j/ai/extension/agent/store/jdbc/JdbcDialect.java` | 新增 | 方言分派（MySQL/H2 vs PostgreSQL 的 DDL 与类型） |
| `.../src/main/java/io/ddd4j/ai/extension/agent/store/jdbc/autoconfigure/JdbcDispatchRepositoryAutoConfiguration.java` | 新增 | `@ConditionalOnProperty` + `@ConditionalOnBean(DataSource)`，按需建表 |
| `.../src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | 新增 | 注册上面的自动装配（**漏了它装配不生效**，既有扩展都有这个文件） |
| `ddd4j-ai-extensions/pom.xml` | 改 | `<subprojects>` 增加子模块（F6） |
| `ddd4j-ai-bom/pom.xml` | 改 | `<dependencyManagement>` 增加条目（F6） |
| `.../src/main/java/io/ddd4j/ai/extension/agent/store/jdbc/package-info.java` | 新增 | 与既有扩展一致 |
| `.../src/test/java/.../JdbcAgentDispatchTaskRepositoryTest.java` | 新增 | Testcontainers MySQL + PostgreSQL 双库契约测试 |
| `.../src/test/java/.../JdbcDialectTest.java` | 新增 | 方言 DDL 分派 |
| `.../src/test/java/.../JdbcDispatchRepositoryAutoConfigurationTest.java` | 新增 | 装配分支 |
| `ddd4j-ai-extension-agent/src/test/java/.../dispatch/AgentDispatchTaskRepositoryContract.java` | 新增 | 共享契约测试基（抽象类） |
| `ddd4j-ai-extension-agent/src/test/java/.../dispatch/InMemoryAgentDispatchTaskRepositoryTest.java` | 新增 | 继承契约基类，锚定既有内存实现 |
| `ddd4j-ai-extension-agent/pom.xml` | 改 | 加 `maven-jar-plugin` 的 **test-jar** goal（见下） |

**共享契约基类的跨模块复用机制（必须做，否则新模块引用不到）**：
测试类默认不随主构件发布，放在 `src/test` 的基类**无法**被另一个模块引用。做法是发布 test-jar：

1. `ddd4j-ai-extension-agent/pom.xml` 加 `maven-jar-plugin` 执行 `test-jar` goal（生成 `ddd4j-ai-extension-agent-<version>-tests.jar`）。
2. 新模块以 `<type>test-jar</type>` + `<scope>test</scope>` 依赖它。

若不想引入 test-jar 构建配置，退而求其次是**在新模块内复制一份契约断言**（DRY 受损但零构建复杂度）。本设计按 test-jar 展开——它是 Maven 标准做法，且能真正保证"三个实现跑同一套断言"。

**测试类命名约束（容易踩的坑）**：本仓 surefire 只收 `*Test` 结尾的类（既有 `MysqlStateStoreWiringTest`、`PostgresChatMemoryIntegrationTest` 都如此），且**未配置 failsafe**。因此 Testcontainers 用例必须命名为 `...Test` 而**非** `...IT`——叫 `*IT` 会导致它**静默不运行**，给出骗人的绿灯。

**Testcontainers 版本**：沿用本扩展既有的 `org.testcontainers:mysql:1.20.6`（由上游 BOM 管理，**不写版本号**）；PostgreSQL 容器依赖同理加 `org.testcontainers:postgresql` 与 `org.postgresql:postgresql`（后者由 Boot BOM 管理）。无 Docker 时靠 Testcontainers 的 `disabledWithoutDocker` 语义跳过（沿用既有行为）。
| `.../src/main/java/.../AgentDispatchTaskRepositoryContract.java`（放 agent 扩展） | 新增 | 见 §7：共享契约测试基 |

**注意**：本项是本序列中**第一个改动 pom 的项**（新增模块必然要动聚合器与 BOM）。第 1、2 项的"不改 pom"约束仅适用于那两项自身。

本项涉及 pom 改动共 3 处：新模块 pom（新增）、`ddd4j-ai-extensions` 聚合器（登记 `<subproject>`）、`ddd4j-ai-bom`（登记 `dependencyManagement`）；另加 agent 扩展 pom 的 `test-jar` 插件配置（§7）。

## 7. 测试策略

核心手段：把仓储契约抽成**一份共享测试基**，让内存实现与 JDBC 实现跑**同一套断言**，从而保证语义一致。

| 测试 | 位置 | 覆盖 |
|------|------|------|
| `AgentDispatchTaskRepositoryContract` | agent 扩展 test（抽象基类） | save 后 findById 命中；findByPlanId 按 createdAt 升序；不同 planId 互不串；update 覆盖状态与结果；findById 未命中返回 `Optional.empty()` |
| `InMemoryAgentDispatchTaskRepositoryTest` | agent 扩展 test | 继承契约基类（**回归锚点**：既有实现必须继续满足契约） |
| `JdbcAgentDispatchTaskRepositoryIT` | 新模块 test | 继承契约基类，Testcontainers **MySQL 8.0** + **PostgreSQL 16** 两个容器各跑一遍 |
| `JdbcDialectTest` | 新模块 test | 方言 DDL 分派正确（PG 用 TEXT、MySQL 用 CLOB） |
| `JdbcDispatchRepositoryAutoConfigurationTest` | 新模块 test | `repository=jdbc` + DataSource → 装配 JDBC 实现；不配置 → 不装配；`auto-ddl=false` → 不建表 |

### 验收标准

1. 共享契约基类的全部用例在**内存实现**与 **JDBC×MySQL**、**JDBC×PostgreSQL** 三个实现上全部通过。
2. 默认配置（不设 `ddd4j.ai.agent.dispatch.repository`）下装配的仍是内存实现，既有测试零回归。
3. `ddd4j-ai-bom` 与 `ddd4j-ai-extensions` 聚合器已登记新模块，`-pl` 可定位。
4. 两线同步（1.0.x 受 §10 阻塞时先只交付 2.0.x 并如实标注）。

### 构建与验证命令

```bash
export JAVA_HOME=/Users/wandl/Library/Java/JavaVirtualMachines/corretto-17.0.20.1/Contents/Home
cd /Users/wandl/workspaces/workspace-ddd4j/workspace-ddd4j-boot/ddd4j-ai
# 2.0.x 用 Maven 4；新增模块后先 install 依赖它的 agent 扩展，否则 -pl 解析不到
/Users/wandl/tools/apache-maven-4.0.0-rc-6/bin/mvn \
  -pl ddd4j-ai-extensions/ddd4j-ai-extension-agent-store-jdbc -am \
  -Denforcer.skip=true -DskipITs test
```

**前置提醒**：本机磁盘曾因 97% 导致 Testcontainers `ContainerLaunchException`（现已清理至 84%）。若容器类测试失败，**先查磁盘**再怀疑代码。

## 8. 风险与缓解

| 风险 | 等级 | 缓解 |
|------|------|------|
| 新增模块后 `-pl` 解析不到（聚合器/BOM 漏登记） | 中 | F6 已明确两处都要改；验收标准 3 专测此项 |
| `AutoConfiguration.imports` 漏建导致装配静默不生效 | 中 | 列入 §6 落点清单；装配测试覆盖 |
| `CLOB` 在 PostgreSQL 不兼容 | 中 | §5 方言分派；双库 IT 覆盖 |
| 双容器 IT 拖慢 CI | 低 | Testcontainers 在无 Docker 时 `disabledWithoutDocker` 静默跳过（既有行为）；单测路径仍覆盖内存实现 |
| 排序语义漂移（内存按 createdAt，JDBC 若按插入序） | 中 | 契约基类显式锚定 `createdAt` 升序，三方实现共同验证 |
| `spring-jdbc` 与 Boot 4.0.7 版本调解 | 低 | 由 Boot BOM 管理；如异常可退化为裸 JDBC（决策 2 替代路径） |
| 自动建表在生产库权限受限 | 中 | `auto-ddl` 可关（决策 5） |

## 9. 后续（同一模板复制）

Redis、MongoDB 后端各自独立模块，复用 §7 的共享契约基类；RabbitMQ / RocketMQ 走 `AgentDispatchTaskQueue` 新端口的独立设计。

## 10. 前置残留项

1.0.x 线在本机**仍无法构建**（与第 1、2 项同一阻塞）：该分支 `ddd4j-ai-samples/pom.xml:66` 与 `ddd4j-ai-sdk-deps/pom.xml:20/25/30` 存在依赖缺 version 的既有缺陷，且本机 `~/.m2` 无 1.0.x 产物。**修这些 POM 需单独授权**——注意本项自身**必然**要改 pom（新增模块），但改的是**聚合器与 BOM 的登记条目**，与"修 1.0.x 既有缺陷 pom"是两件不同的事，后者仍需授权。
