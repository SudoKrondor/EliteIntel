package elite.intel.companion.llm;

import com.google.gson.JsonObject;
import elite.intel.companion.model.llm.LlmRequest;
import elite.intel.companion.model.llm.LlmResult;

/**
 * Provider-specific adapter: renders an {@link LlmRequest} into the provider's native chat-completion body
 * and parses a raw provider response. This is the seam that lets a new provider plug in as a new
 * implementation; the {@link CompanionLlmGateway} and the rest of companion mode stay provider-neutral.
 * <p>
 * Two response shapes: {@link #parse} for the tool-calling consciousness turn (well-formed tool-calls), and
 * {@link #parseText} for the plain-text compression turn (the model's free-text summary).
 */
public interface LlmProviderAdapter {

    /** Renders the request into the provider's native chat-completion body (JSON string). */
    String buildRequestBody(LlmRequest request);

    /**
     * Parses a raw provider response into a tool-calling result. Returns
     * {@link LlmResult.Status#INVALID_RESPONSE} for anything that is not one or more well-formed tool-calls
     * (plain text, empty, malformed).
     */
    LlmResult parse(JsonObject response);

    /**
     * Extracts the assistant's plain-text content from a raw provider response (compression turn), or
     * {@code null} when absent/blank/malformed.
     */
    String parseText(JsonObject response);
}
