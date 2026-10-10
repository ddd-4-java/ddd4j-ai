/*
 * Copyright (c) 2024-2026 ddd4j project. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.ddd4j.ai.sdk;

/**
 * ddd4j AI 客户端 SDK 聚合模块的文档锚点（无运行时逻辑）。
 *
 * <p>本模块不包含实现代码，仅作为 LLM CLI / 多模态 AI SDK 的依赖聚合入口：业务方引入
 * {@code io.ddd4j.ai:ddd4j-ai-sdk-deps} 即可传递获得以下组件，版本由
 * {@code io.ddd4j.ai:ddd4j-ai-dependencies} BOM 统一管理：
 * <ul>
 *     <li>{@code claudecode-java-sdk}：Anthropic Claude Code CLI 会话调用；</li>
 *     <li>{@code codex-java-sdk}：OpenAI Codex CLI 编码任务执行；</li>
 *     <li>{@code dreamina-java-sdk}：字节跳动 Dreamina 文生图 API。</li>
 * </ul>
 *
 * <p>保留本公共类的原因：javadoc 工具对仅含 {@code package-info.java} 的包会报
 * "No public or protected classes found to document" 错误并使 javadoc 门禁失败；
 * 本类作为模块级文档锚点使门禁可覆盖全部模块（家族不允许在 POM 中关闭 failOnError）。
 */
public final class SdkDependencies {

    private SdkDependencies() {
    }
}
