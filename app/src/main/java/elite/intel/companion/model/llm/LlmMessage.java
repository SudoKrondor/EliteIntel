package elite.intel.companion.model.llm;

import java.util.List;

/**
 * One message in a thought's local message flow / an LLM request, in the OpenAI/Mistral chat format.
 * Distinct from {@code MemoryEntry}: this is the live LLM-protocol transcript of a single turn, not the
 * durable experience timeline (see COMPANION_ARCHITECTURE.md §2.8).
 * <p>
 * Two fields are role-specific: {@code toolCallId} is set only for {@link LlmMessageRole#TOOL} result
 * messages (linking the result to the assistant tool-call that produced it within the current
 * function-calling flow), and {@code toolCalls} carries the tool invocations of an
 * {@link LlmMessageRole#ASSISTANT} turn. The assistant turn must be replayed before its tool results so
 * the next round is a protocol-valid {@code assistant(tool_calls) -> tool(result)} pair.
 *
 * @param role        message role
 * @param content     message text content (null for assistant turns that only carry tool-calls)
 * @param toolCallId  tool_call_id for tool-result messages; null otherwise
 * @param toolCalls   tool invocations for an assistant tool-call turn; empty otherwise
 */
public record LlmMessage(
        LlmMessageRole role,
        String content,
        String toolCallId,
        List<LlmToolInvocation> toolCalls
) {
    public LlmMessage {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    /** Convenience for non-tool messages (no tool_call_id, no tool-calls). */
    public static LlmMessage of(LlmMessageRole role, String content) {
        return new LlmMessage(role, content, null, List.of());
    }

    /** Assistant turn that carries only tool-calls (no text content); replayed before its tool results. */
    public static LlmMessage assistantToolCalls(List<LlmToolInvocation> toolCalls) {
        return new LlmMessage(LlmMessageRole.ASSISTANT, null, null, toolCalls);
    }

    /** Tool-result message answering one assistant tool-call by its {@code tool_call_id}. */
    public static LlmMessage toolResult(String toolCallId, String content) {
        return new LlmMessage(LlmMessageRole.TOOL, content, toolCallId, List.of());
    }
}
