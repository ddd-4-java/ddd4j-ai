package io.ddd4j.ai.samples.integrated.smartagent;

import io.ddd4j.ai.extension.agent.service.AgentResult;
import io.ddd4j.ai.extension.agent.service.AgentTask;
import io.ddd4j.ai.extension.mcp.service.McpToolProvider;
import io.ddd4j.ai.extension.rag.service.RagService;
import io.ddd4j.ai.extension.document.DocumentReader;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 场景 3：完整智能体（agent + document + rag + mcp + memory）。
 * <p>
 * 展示智能体的全部能力：工具调用、知识库检索、文档理解、记忆。
 */
@Component
public class SmartAgentSample {

    private final HarnessAgent harnessAgent;
    private final McpToolProvider mcpToolProvider;
    private final RagService ragService;
    private final DocumentReader documentReader;

    public SmartAgentSample(HarnessAgent harnessAgent, McpToolProvider mcpToolProvider,
                            RagService ragService, DocumentReader documentReader) {
        this.harnessAgent = harnessAgent;
        this.mcpToolProvider = mcpToolProvider;
        this.ragService = ragService;
        this.documentReader = documentReader;
    }

    /**
     * 基础智能体：HarnessAgent 直接执行（含内置 ReAct 循环、工具调用、记忆）。
     */
    public String run(String instruction) {
        return harnessAgent.call(new UserMessage(instruction)).block().getTextContent();
    }

    /**
     * 带知识库的智能体：先检索相关知识注入上下文，再让智能体推理。
     */
    public String runWithKnowledge(String question) {
        String context = ragService.query(question);
        String augmented = "参考资料：\n" + context + "\n\n问题：" + question;
        return harnessAgent.call(new UserMessage(augmented)).block().getTextContent();
    }

    /**
     * 文档理解 + 智能体：读取文档内容作为上下文，让智能体分析。
     */
    public String analyzeDocument(File file) throws Exception {
        var doc = documentReader.read(file);
        String instruction = "请分析以下文档内容：\n\n" + doc.fullMarkdown()
                + "\n\n请给出文档摘要、关键信息和建议。";
        return harnessAgent.call(new UserMessage(instruction)).block().getTextContent();
    }

    /**
     * 列出当前可用的 MCP 工具（供前端展示或调试）。
     */
    public List<Map<String, Object>> availableTools() {
        return mcpToolProvider.availableTools().stream()
                .map(t -> Map.<String, Object>of(
                        "name", t.name(),
                        "description", t.description(),
                        "parameters", t.parameters()))
                .toList();
    }

    /**
     * 异步执行（Mono 语义）。
     */
    public Mono<String> runAsync(String instruction) {
        return harnessAgent.call(new UserMessage(instruction))
                .map(msg -> msg.getTextContent());
    }
}
