package elite.intel.companion.llm;

import com.google.gson.JsonObject;
import elite.intel.companion.model.llm.LlmRequest;
import elite.intel.companion.model.llm.LlmResult;

/**
 * Provider-specific wire format for native tool-calling: renders an {@link LlmRequest} into a request
 * body and parses a raw provider response into an {@link LlmResult}. This is the seam that lets a new
 * provider plug in as a new implementation; the {@link CompanionLlmGateway} and the rest of companion
 * mode stay provider-neutral.
 * <p>
 * Parsing only concerns the wire shape (are there well-formed tool-calls?); semantic validation that a
 * called tool was actually offered is done provider-neutrally by the gateway.
 */
public interface CompanionLlmDialect {

    /** Renders the request into the provider's native chat-completion body (JSON string). */
    String buildRequestBody(LlmRequest request);

    /**
     * Parses a raw provider response into a result. Returns {@link LlmResult.Status#INVALID_RESPONSE}
     * for anything that is not one or more well-formed tool-calls (plain text, empty, malformed).
     */
    LlmResult parse(JsonObject response);
}
