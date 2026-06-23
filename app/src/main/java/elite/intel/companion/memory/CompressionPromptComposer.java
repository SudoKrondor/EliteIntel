package elite.intel.companion.memory;

import elite.intel.companion.model.llm.LlmMessage;
import elite.intel.companion.model.llm.LlmMessageRole;
import elite.intel.companion.model.memory.MemoryEntry;

import java.util.List;
import java.util.Locale;

/**
 * Builds the message flow for a mid-term to long-term compression turn - the compression counterpart of the
 * consciousness {@code PromptComposer}, kept separate from the {@code MidTermToLongTermConsolidator}'s
 * orchestration. Plain-text turn: an English system instruction plus a user block carrying the existing
 * summary and the buffered entries to merge. No singletons/localization, so it is directly testable.
 */
final class CompressionPromptComposer {

    private static final String INSTRUCTION =
            "You compress old crew memory into a single compact running summary. "
                    + "Merge the new entries into the existing summary, keep what stays relevant, drop trivia, "
                    + "and reply with only the updated summary as plain text, at most "
                    + CompanionMemoryLimits.SUMMARY_MAX_CHARS + " characters.";

    /** Returns [system instruction, user(existing summary + new entries)] for the compression request. */
    List<LlmMessage> compose(String currentSummary, List<MemoryEntry> batch) {
        StringBuilder user = new StringBuilder();
        user.append("Existing summary:\n")
                .append(currentSummary == null || currentSummary.isBlank() ? "(none)" : currentSummary.strip());
        user.append("\n\nNew entries to merge:\n");
        for (MemoryEntry entry : batch) {
            user.append('[').append(entry.source().name()).append("][").append(topicId(entry)).append("] ")
                    .append(entry.content()).append('\n');
        }
        return List.of(
                LlmMessage.of(LlmMessageRole.SYSTEM, INSTRUCTION),
                LlmMessage.of(LlmMessageRole.USER, user.toString()));
    }

    private static String topicId(MemoryEntry entry) {
        return entry.topic().name().toLowerCase(Locale.ROOT);
    }
}
