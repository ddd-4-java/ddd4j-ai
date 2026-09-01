package io.ddd4j.ai.extension.agent.agent;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.ToolCallParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Mono;

/**
 * ddd4j-ai-extension-chat 暴露的 Spring AI {@link ToolCallback} 到 Agentscope
 * {@link AgentTool} 的适配器（参考 cloud-agents {@code AgentScopeOpenClawToolAdapter}）。
 * <p>
 * 调用映射：{@code ToolCallback.call(inputJson)} → JSON 解析为 Map → Agentscope
 * {@link ToolCallParam#getInput()}；结果字符串 → {@link ToolResultBlock#text(String)}；
 * 异常 → {@link ToolResultBlock#error(String)}。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public final class SpringAiToolkitBuilder {

    private static final Logger log = LoggerFactory.getLogger(SpringAiToolkitBuilder.class);

    private SpringAiToolkitBuilder() {
    }

    /**
     * 把 Spring AI 工具回调注册到 Agentscope Toolkit 工厂方法。
     * 单个工具注册失败时记录 WARN 跳过（不中断整个 Toolkit 装配），与 cloud-agents 行为一致。
     */
    public static void registerSpringAiTools(List<ToolCallback> callbacks,
                                            io.agentscope.core.tool.Toolkit toolkit) {
        if (callbacks == null || callbacks.isEmpty()) {
            return;
        }
        Objects.requireNonNull(toolkit, "toolkit");
        for (ToolCallback callback : callbacks) {
            try {
                if (callback.getToolDefinition() == null) {
                    log.warn("register tool failed, skip: callback has no tool definition");
                    continue;
                }
                toolkit.registerAgentTool(new SpringAiAgentTool(callback));
            } catch (RuntimeException e) {
                log.warn("register tool failed, skip: name={}, error={}",
                        callback.getToolDefinition().name(), e.getMessage());
            }
        }
    }

    private static final class SpringAiAgentTool implements AgentTool {

        private final ToolCallback callback;

        private SpringAiAgentTool(ToolCallback callback) {
            this.callback = callback;
        }

        @Override
        public String getName() {
            return callback.getToolDefinition().name();
        }

        @Override
        public String getDescription() {
            return callback.getToolDefinition().description();
        }

        @Override
        public Map<String, Object> getParameters() {
            // 解析 ToolCallback JSON Schema 为 Map（Agentscope 参数协议）
            // Tika 不暴露 JSON schema 解析时返回空 Map（参数经 ToolCallParam.getInput() 走 raw JSON）
            String schema = callback.getToolDefinition().inputSchema();
            if (schema == null || schema.isBlank()) {
                return Collections.emptyMap();
            }
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper =
                        new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(schema, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.debug("tool schema not parseable as map: name={}, schema={}", getName(), schema);
                return Collections.emptyMap();
            }
        }

        @Override
        public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
            if (param == null || param.getInput() == null || param.getInput().isEmpty()) {
                return Mono.fromCallable(() -> ToolResultBlock.text(callWithArgs("{}")))
                        .onErrorResume(e -> Mono.just(ToolResultBlock.error(
                                "tool execution failed: " + e.getMessage())));
            }
            String inputJson;
            try {
                inputJson = new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(param.getInput());
            } catch (Exception e) {
                return Mono.just(ToolResultBlock.error("input serialize failed: " + e.getMessage()));
            }
            return Mono.fromCallable(() -> callWithArgs(inputJson))
                    .map(ToolResultBlock::text)
                    .onErrorResume(e -> Mono.just(ToolResultBlock.error(
                            "tool execution failed: " + e.getMessage())));
        }

        private String callWithArgs(String inputJson) {
            try {
                Object result = callback.call(inputJson);
                return result == null ? "" : result.toString();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
