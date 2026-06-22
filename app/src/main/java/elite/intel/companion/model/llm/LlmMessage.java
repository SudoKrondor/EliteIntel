package elite.intel.companion.model.llm;

/**
 * One message in a thought's local message flow / an LLM request, in the OpenAI/Mistral chat format.
 * Distinct from {@code MemoryEntry}: this is the live LLM-protocol transcript of a single turn, not the
 * durable experience timeline (see COMPANION_ARCHITECTURE.md §2.8).
 * <p>
 * {@code toolCallId} is set only for {@link LlmMessageRole#TOOL} result messages, linking the result to
 * the assistant tool-call that produced it within the current function-calling flow.
 *
 * @param role        message role
 * @param content     message text content (may be null for assistant turns that only carry tool-calls)
 * @param toolCallId  tool_call_id for tool-result messages; null otherwise
 */
public record LlmMessage(
        LlmMessageRole role,
        String content,
        String toolCallId
) {
    /** Convenience for non-tool messages (no tool_call_id). */
    public static LlmMessage of(LlmMessageRole role, String content) {
        return new LlmMessage(role, content, null);
    }
}
