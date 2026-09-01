package io.ddd4j.ai.extension.flow.service;

import java.util.Map;

/**
 * 工作流节点类型。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public enum FlowNodeType {

    /** 大模型节点：以 prompt（可含 {stateKey} 占位符）调对话端口，结果写入 outputKey。 */
    LLM,

    /** 工具节点：以 inputKey 的 state 值为入参执行 ToolCallback，结果写入 outputKey。 */
    TOOL,

    /** 分支节点：按 inputKey 的 state 值经 branches 映射路由到下一节点。 */
    BRANCH
}
