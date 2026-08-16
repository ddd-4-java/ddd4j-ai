package io.ddd4j.ai.sample;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * ddd4j-ai 可运行示例应用：Ollama 模型 + chat/memory/embedding/vectordb/rag 全组件演示。
 *
 * <p>启动前提：本地（或 SPRING_AI_OLLAMA_BASE_URL 指向的）Ollama 服务，且已拉取
 * {@code qwen2.5:0.5b}（chat）与 {@code all-minilm}（embedding）模型：
 * <pre>
 *   ollama pull qwen2.5:0.5b
 *   ollama pull all-minilm
 * </pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@SpringBootApplication
public class SampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }

    /**
     * 演示用内存向量库：让 vectordb/rag 组件在无外部向量数据库时也能完整装配。
     * 生产环境请移除此 bean 并引入 spring-ai-starter-vector-store-pgvector 等后端。
     */
    @Bean
    VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
