package io.ddd4j.ai.core;

/**
 * Minimal AI invocation handler contract.
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public interface AiHandler extends AiComponent {

    /**
     * Handles a request and returns a normalized response.
     *
     * @param request request
     * @return response
     */
    AiResponse handle(AiRequest request);
}
