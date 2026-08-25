# MCP 模型上下文协议组件设计

- 日期：2026-08-12
- 作者：PartMe.AI
- 状态：已实施（2026-08-25 组件补全批次）
- 范围：`ddd4j-ai-extension-mcp` —— Model Context Protocol 工具/资源接入
- 关联文档：整体架构见 `2026-08-07-ddd4j-ai-architecture-design.md`；被 agent 引用

---

## 1. 背景

[MCP](https://modelcontextprotocol.io/) 是工具调用的标准化协议，统一 Tool/Resource/Prompt 三类能力的暴露与消费。mcp 组件集成 MCP SDK，为 agent 提供跨进程、跨语言的工具接入能力，并支持 Spring Boot AutoConfiguration 开箱接入。

## 2. 目标

- 集成 MCP SDK（BOM 治理 `mcp-bom:2.0.0`）。
- 提供 Tool / Resource / Prompt 三类能力的暴露与调用端口。
- 提供 Spring Boot AutoConfiguration，业务侧声明即生效。
- 与 agent 组件协同（agent 通过 mcp 端口调用工具）。

### 非目标

- 不实现具体业务工具（由业务服务作为 MCP server 暴露）。
- 不替代 Spring AI function calling（二者并存，mcp 面向跨进程标准协议）。

## 3. 关键决策（建议）

| # | 决策候选 | 理由 |
|---|---------|------|
| M1 | 客户端（消费工具）+ 服务端（暴露工具）双向支持 | 既能调用外部 MCP server，也能把 ddd4j 能力暴露 |
| M2 | AutoConfiguration 按 starter 习惯装配 | 业务零配置接入 |
| M3 | 工具描述符（input schema）自动生成 | 降低业务暴露工具成本 |

## 4. 总体架构

```
agent → McpClientService（端口）→ MCP SDK → 外部 MCP Server
业务   → McpServerService（端口）→ MCP SDK → 暴露 Tool/Resource/Prompt
```

## 5. 接口方向

- `McpClientService`：`callTool(name, args)` → `result`；`listTools()`。
- `McpServerService`：`exposeTool(ToolSpec)`。

## 6. 模块落点

```
ddd4j-ai-extension-mcp/src/main/java/io/ddd4j/ai/cmpt/mcp
├── properties/ (McpProperties: server url/传输方式)
├── service/{McpClientService, McpServerService}.java
├── service/impl/...
└── autoconfigure/ (Spring Boot 自动装配)
```

## 7. 依赖

- MCP SDK 2.0.0（`mcp-bom`）
- Spring Boot AutoConfiguration

## 8. 测试策略

- 客户端调用回路测试（嵌入式 MCP server）；AutoConfiguration 装配测试；工具 schema 生成校验。
