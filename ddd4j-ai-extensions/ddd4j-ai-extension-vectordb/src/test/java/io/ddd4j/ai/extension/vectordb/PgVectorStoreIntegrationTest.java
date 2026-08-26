package io.ddd4j.ai.extension.vectordb;

import io.ddd4j.ai.extension.vectordb.service.VectorDbService;
import io.ddd4j.ai.extension.vectordb.service.impl.VectorStoreAdapter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.DriverManager;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 向量数据库组件 pgvector 容器集成测试（镜像 pgvector/pgvector:pg16，经 PostgreSQLContainer 驱动）：
 * 真实 PgVectorStore 后端验证 VectorStoreAdapter 的入库/检索/删除。无 Docker 自动跳过。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Testcontainers(disabledWithoutDocker = true)
class PgVectorStoreIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> PGVECTOR = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    /** 确定性二维嵌入：[首字符编码, 常量]。 */
    static class FirstCharEmbeddingModel implements org.springframework.ai.embedding.EmbeddingModel {

        @Override
        public float[] embed(String text) {
            char c = text.isEmpty() ? 'a' : text.charAt(0);
            return new float[]{c, 100.0f};
        }

        @Override
        public float[] embed(Document document) {
            return embed(document.getText());
        }

        @Override
        public org.springframework.ai.embedding.EmbeddingResponse call(
                org.springframework.ai.embedding.EmbeddingRequest request) {
            List<org.springframework.ai.embedding.Embedding> embeddings = new java.util.ArrayList<>();
            var instructions = request.getInstructions();
            for (int i = 0; i < instructions.size(); i++) {
                embeddings.add(new org.springframework.ai.embedding.Embedding(
                        embed(instructions.get(i)), i));
            }
            return new org.springframework.ai.embedding.EmbeddingResponse(embeddings);
        }
    }

    static JdbcTemplate jdbcTemplate;

    static VectorDbService vectorDb;

    @BeforeAll
    static void setUp() throws java.sql.SQLException {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriver(DriverManager.getDriver(PGVECTOR.getJdbcUrl()));
        dataSource.setUrl(PGVECTOR.getJdbcUrl());
        dataSource.setUsername(PGVECTOR.getUsername());
        dataSource.setPassword(PGVECTOR.getPassword());

        jdbcTemplate = new JdbcTemplate(dataSource);
        // pgvector 镜像自带扩展二进制，但需显式激活后 vector 类型才可用
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");

        PgVectorStore store = PgVectorStore.builder(jdbcTemplate, new FirstCharEmbeddingModel())
                .dimensions(2)
                .distanceType(PgVectorStore.PgDistanceType.EUCLIDEAN_DISTANCE)
                .initializeSchema(true)
                .build();
        // 手动构造时需自行触发 schema 初始化（容器装配场景由 Spring 回调）
        store.afterPropertiesSet();
        vectorDb = new VectorStoreAdapter(store, 4);
    }

    @BeforeEach
    void cleanTable() {
        // 测试方法共享同一容器实例，逐用例清空避免数据残留干扰排序断言
        jdbcTemplate.execute("TRUNCATE TABLE public.vector_store");
    }

    @Test
    void addThenSearchFindsSimilarDocuments() {
        vectorDb.add(List.of(
                new Document("apple pie recipe", Map.of("kind", "food")),
                new Document("banana bread recipe", Map.of("kind", "food"))));

        List<Document> hits = vectorDb.search("apple", 2, null);

        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).getText()).contains("apple");
    }

    @Test
    void searchAppliesMetadataFilterOnPgVector() {
        vectorDb.add(List.of(
                new Document("avocado 牛油果", Map.of("kind", "fruit")),
                new Document("anchor 锚点", Map.of("kind", "tool"))));

        var b = new org.springframework.ai.vectorstore.filter.FilterExpressionBuilder();
        List<Document> hits = vectorDb.search("avocado", 4, b.eq("kind", "tool").build());

        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).getText()).contains("anchor");
    }

    @Test
    void deleteRemovesFromPgVector() {
        Document doc = new Document("temp 临时文档", Map.of());
        vectorDb.add(List.of(doc));

        vectorDb.delete(List.of(doc.getId()));

        assertThat(vectorDb.search("temp", 4, null)).isEmpty();
    }
}
