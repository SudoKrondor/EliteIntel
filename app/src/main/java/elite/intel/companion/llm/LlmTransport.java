package elite.intel.companion.llm;

import com.google.gson.JsonObject;

/**
 * Provider-specific transport: sends a rendered request body to the LLM endpoint and returns the raw
 * parsed JSON response. Kept as a seam so the gateway's logic (render, retry, parse) can be unit-tested
 * without the network, and so a provider's existing client owns its endpoint/auth/usage accounting.
 */
@FunctionalInterface
public interface LlmTransport {

    /** Sends the request body and returns the raw JSON response (an error object on failure). */
    JsonObject send(String requestBody);
}
