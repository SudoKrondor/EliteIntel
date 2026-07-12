package elite.intel.companion.llm;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.AiTransportResult;

/**
 * Provider-specific transport: sends a rendered request body to the LLM endpoint. New production transports
 * override {@link #sendOutcome(String)} to return typed HTTP failures; the JSON-only method remains a compact
 * success-only test seam.
 */
@FunctionalInterface
public interface LlmTransport {

    /** Sends the request body and returns a raw JSON response for success-only test transports. */
    JsonObject send(String requestBody);

    /**
     * Sends the request body with a typed transport result. Implementations backed by legacy scripted JSON can
     * rely on the default success outcome; production bridges override this method to preserve HTTP failure kind.
     */
    default AiTransportResult sendOutcome(String requestBody) {
        return AiTransportResult.success(send(requestBody));
    }
}
