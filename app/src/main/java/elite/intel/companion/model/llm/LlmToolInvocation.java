package elite.intel.companion.model.llm;

import com.google.gson.JsonObject;

/**
 * One tool invocation the LLM requested in its response (one entry of the assistant's tool_calls).
 *
 * @param id         provider-issued {@code tool_call_id}; links this invocation to its tool result
 *                   inside the current message flow only (not part of long-term memory identity)
 * @param name       name of the tool to invoke
 * @param arguments  parsed JSON arguments
 */
public record LlmToolInvocation(
        String id,
        String name,
        JsonObject arguments
) {}
