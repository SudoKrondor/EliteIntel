package elite.intel.companion.model.llm;

import java.util.List;

/**
 * Unit of work handed to {@code LlmGateway}. The gateway never sees a {@code Thought}; it only knows
 * requests, keyed by {@code requestId}, and returns a handle.
 *
 * @param requestId  unique id for correlation/diagnostics
 * @param messages   full message flow to send
 * @param tools      native tool-calling tool set (may be empty for compression mode)
 * @param profile    prompt cache profile (drives prompt_cache_key and tool-call expectation)
 */
public record LlmRequest(
        String requestId,
        List<LlmMessage> messages,
        List<LlmToolDefinition> tools,
        PromptCacheProfile profile
) {}
