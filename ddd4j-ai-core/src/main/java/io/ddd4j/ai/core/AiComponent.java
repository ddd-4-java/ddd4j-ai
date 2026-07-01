package io.ddd4j.ai.core;

/**
 * Common contract for AI capability components.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public interface AiComponent {

    /**
     * Stable component name used for routing, diagnostics, and configuration.
     *
     * @return component name
     */
    String name();
}
