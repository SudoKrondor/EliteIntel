package elite.intel.companion.memory;

import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.companion.model.llm.LlmMessage;
import elite.intel.companion.model.llm.LlmMessageRole;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

import java.util.List;
import java.util.Locale;

/**
 * Builds the message flow for a mid-term to long-term compression turn - the compression counterpart of the
 * consciousness {@code PromptComposer}, kept separate from the {@code MidTermToLongTermConsolidator}'s
 * orchestration. Plain-text turn: an English system instruction plus a user block carrying the existing
 * summary and the buffered entries to merge.
 * <p>
 * Like the main prompt, the instruction itself is English, but it names the commander's language (same
 * source as {@code CompanionSystemPromptPart}) so the summary is written in that language - memory content
 * is stored in the commander's language and the summary is re-injected into the prompt.
 */
final class CompressionPromptComposer {

    private static final String INSTRUCTION =
            "You compress old crew memory into a single compact running summary. "
                    + "Merge the new entries into the existing summary, keep what stays relevant, drop trivia, "
                    + "and preserve entries marked [high] importance faithfully while condensing the rest. "
                    + "Reply with only the updated summary as plain text, at most "
                    + CompanionMemoryLimits.SUMMARY_MAX_CHARS + " characters.";

    /**
     * Returns [system instruction, user(content)] for shrinking a single over-long memory entry to a short
     * gist (the prompt-bloat guard, run by {@link OversizedMemoryCompressor}, which caps the stored gist at the
     * entry size limit). Plain-text turn; the gist stays in the commander's language.
     */
    List<LlmMessage> composeLineCompression(String content) {
        // The line being shrunk is a SPOKEN answer (numbers spelled out in words), which wastes space; memory is
        // plain text, so use digits and drop speech padding. Accuracy over brevity: a small model must not be
        // invited to re-round or restate a figure (it once turned 44M into 440M), so keep every number verbatim.
        String instruction = "Rewrite the crew memory line below as ONE short sentence (about 15 words) that keeps "
                + "only its single most important point and drops secondary details, enumerations and coordinates. "
                + "Keep every fact and number you include exactly as in the source - never invent, change, "
                + "re-estimate or exaggerate it. Write numbers as digits, not spelled-out words. Reply with only "
                + "that sentence as plain text: no preamble, no list, no quotes, no line breaks. " + languageRule()
                + " Example of the target form: \"Lembava: independent high-tech system with bounty-hunting sites "
                + "and a conflict zone.\"";
        return List.of(
                LlmMessage.of(LlmMessageRole.SYSTEM, instruction),
                LlmMessage.of(LlmMessageRole.USER, content == null ? "" : content.strip()));
    }

    /** Returns [system instruction, user(existing summary + new entries)] for the compression request. */
    List<LlmMessage> compose(String currentSummary, List<MemoryEntry> batch) {
        StringBuilder user = new StringBuilder();
        user.append("Existing summary:\n")
                .append(currentSummary == null || currentSummary.isBlank() ? "(none)" : currentSummary.strip());
        user.append("\n\nNew entries to merge:\n");
        for (MemoryEntry entry : batch) {
            user.append('[').append(entry.source().name()).append("][").append(topicId(entry)).append("][")
                    .append(entry.importance().name().toLowerCase(Locale.ROOT)).append("] ")
                    .append(entry.content()).append('\n');
        }
        return List.of(
                LlmMessage.of(LlmMessageRole.SYSTEM, INSTRUCTION + " " + languageRule()),
                LlmMessage.of(LlmMessageRole.USER, user.toString()));
    }

    /** Names the commander's language (same source as the consciousness prompt) and binds the summary to it. */
    private static String languageRule() {
        Language language = AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(SystemSession.getInstance());
        String name = language.displayName();
        return "The memory content is in " + name + "; write the summary in " + name + ".";
    }

    private static String topicId(MemoryEntry entry) {
        return entry.topic().id();
    }
}
