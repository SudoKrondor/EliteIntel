package elite.intel.ai.brain.vega.model.llm;

import java.util.List;
import java.util.Objects;

/**
 * Unit of work handed to {@code LlmGateway}. The gateway never sees a {@code Thought}; it only knows
 * requests, keyed by {@code requestId}, and returns a handle.
 *
 * @param requestId    unique id for correlation/diagnostics
 * @param messages     full message flow to send
 * @param tools        native tool-calling tool set (may be empty for compression mode)
 * @param profile      prompt cache profile (drives prompt_cache_key and tool-call expectation)
 * @param trace        owning thought's diagnostic trace, so a gateway-internal repair/retry can be attributed to
 *                     the same thought on the SYSTEM LOG surface; null for callers with no thought (compression,
 *                     key generation, tests)
 * @param maxToolCalls how many calls the caller can settle in this one round, which is what makes a larger
 *                     response a defect rather than a bonus. Only a turn that really executes several calls
 *                     raises it; everything else stays at one, so a stray extra call is still repaired away
 *                     instead of being silently dropped by a caller that reads the first invocation
 */
public record LlmRequest(
        String requestId,
        List<LlmMessage> messages,
        List<LlmToolDefinition> tools,
        PromptCacheProfile profile,
        String trace,
        int maxToolCalls
) {
    /** Freezes the request collections before asynchronous render/send/validate processing starts. */
    public LlmRequest {
        messages = List.copyOf(Objects.requireNonNull(messages, "messages"));
        tools = List.copyOf(Objects.requireNonNull(tools, "tools"));
        if (maxToolCalls < 1) {
            throw new IllegalArgumentException("maxToolCalls must be at least 1");
        }
    }

    /** Back-compat constructor for call sites that carry no diagnostic trace (compression, key generation, tests). */
    public LlmRequest(String requestId, List<LlmMessage> messages, List<LlmToolDefinition> tools, PromptCacheProfile profile) {
        this(requestId, messages, tools, profile, null, 1);
    }
}
