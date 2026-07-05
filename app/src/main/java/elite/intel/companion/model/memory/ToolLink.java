package elite.intel.companion.model.memory;

/**
 * Optional tool-call linkage carried by a {@link MemoryEntry}, so the timeline can be replayed into a
 * protocol-valid {@code assistant(tool_calls) -> tool(result)} pair (see COMPANION_ARCHITECTURE.md §2.8/§2.10).
 * <p>
 * Two shapes, distinguished by {@link Kind}:
 * <ul>
 *   <li>{@link Kind#CALL} - the model's own function call, recorded on a {@link MemorySource#COMPANION} entry:
 *       carries the tool {@link #toolName()} and its {@link #argumentsJson()} plus the correlating
 *       {@link #toolCallId()}; the entry's content is unused for the wire message.</li>
 *   <li>{@link Kind#RESULT} - the function's return, recorded on a {@link MemorySource#TOOL_RESULT} entry:
 *       carries only the matching {@link #toolCallId()}; the entry's content is the result text.</li>
 * </ul>
 * The {@code toolCallId} is a synthesized correlation id (local models often omit provider ids), stable only
 * within the replayed flow - it is not part of the entry's durable identity.
 *
 * @param kind          whether this links an assistant call or its tool result
 * @param toolCallId    correlation id pairing a CALL with its RESULT
 * @param toolName      the invoked tool's name (CALL only; null for RESULT)
 * @param argumentsJson the call's arguments as compact JSON (CALL only; null for RESULT)
 */
public record ToolLink(Kind kind, String toolCallId, String toolName, String argumentsJson) {

    public enum Kind { CALL, RESULT }

    /** An assistant tool-call link: the model invoked {@code toolName} with {@code argumentsJson} under {@code id}. */
    public static ToolLink call(String toolCallId, String toolName, String argumentsJson) {
        return new ToolLink(Kind.CALL, toolCallId, toolName, argumentsJson);
    }

    /** A tool-result link: the result answering the CALL with the same {@code toolCallId}. */
    public static ToolLink result(String toolCallId) {
        return new ToolLink(Kind.RESULT, toolCallId, null, null);
    }

    public boolean isCall() {
        return kind == Kind.CALL;
    }

    public boolean isResult() {
        return kind == Kind.RESULT;
    }
}
