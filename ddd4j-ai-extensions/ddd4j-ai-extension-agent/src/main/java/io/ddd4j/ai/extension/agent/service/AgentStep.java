package io.ddd4j.ai.extension.agent.service;

import java.beans.ConstructorProperties;

import java.util.Objects;

/**
 * 智能体执行中的一步：思考 / 动作 / 观察 / 结果（流式与诊断用）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */

public final class AgentStep {

    private static final long serialVersionUID = 0L;

    private final String type;

    private final String content;

    private final int index;

    /**
 * @param type 步骤类型：plan / thought / action / observation / execution / result
 * @param content 步骤内容
 * @param index 步骤序号（0 起）
 */

    @ConstructorProperties({ "type", "content", "index" })
    public AgentStep(String type, String content, int index) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type must not be blank");
        }
        this.type = type;
        this.content = content;
        this.index = index;
    }

    public String type() {
        return type;
    }

    public String content() {
        return content;
    }

    public int index() {
        return index;
    }

    public String getType() {
        return type();
    }

    public String getContent() {
        return content();
    }

    public int getIndex() {
        return index();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (Objects.isNull(obj) || getClass() != obj.getClass()) {
            return false;
        }
        AgentStep other = (AgentStep) obj;
        return Objects.equals(this.type, other.type) && Objects.equals(this.content, other.content) && this.index == other.index;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Objects.hashCode(type);
        result = 31 * result + Objects.hashCode(content);
        result = 31 * result + Integer.hashCode(index);
        return result;
    }

    @Override
    public String toString() {
        return "AgentStep[type=" + type + ", content=" + content + ", index=" + index + "]";
    }
}
