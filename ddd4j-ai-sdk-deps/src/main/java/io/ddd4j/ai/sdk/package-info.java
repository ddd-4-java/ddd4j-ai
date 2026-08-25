/**
 * ddd4j-ai SDK 客户端聚合模块的包占位。
 *
 * <p>本模块不包含任何实现代码，仅作为 LLM CLI / 多模态 AI SDK 的依赖聚合入口：
 * 业务方引入 {@code io.ddd4j.ai:ddd4j-ai-sdk-deps} 即可传递获得 claudecode-java-sdk
 * （Anthropic Claude Code CLI）、codex-java-sdk（OpenAI Codex CLI）、
 * dreamina-java-sdk（字节跳动 Dreamina 文生图）等 AI 组件，
 * 版本由 {@code io.ddd4j.ai:ddd4j-ai-dependencies} BOM 统一管理。
 *
 * <p>原 {@code io.ddd4j:ddd4j-ai-dependencies} 模块（ddd4j 主仓）按 #841 迁移至此：
 * 包名由 {@code io.ddd4j.ai.dependencies} 改为 {@code io.ddd4j.ai.sdk}，groupId
 * 由 {@code io.ddd4j} 改为 {@code io.ddd4j.ai}，与 ddd4j-ai 项目整体保持一致。
 */
package io.ddd4j.ai.sdk;
