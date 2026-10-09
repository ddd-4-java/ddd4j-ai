package io.ddd4j.ai.core;

/**
 * 通用 AI 能力组件契约，所有可插拔 AI 组件的最小公共接口。
 *
 * @author ddd4j
 */
public interface AiComponent {

    /**
     * 稳定的组件名称，用于路由选择、诊断排查与配置识别。
     *
     * @return 组件名称
     */
    String name();
}
