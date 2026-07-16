package elite.intel.companion.memory;

import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.companion.model.llm.LlmMessage;
import elite.intel.companion.model.llm.LlmMessageRole;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryKind;
import elite.intel.companion.model.memory.MemoryRecord;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

import java.util.List;

/** Builds the plain-text LLM request that consolidates old records of one memory kind. */
final class CompressionPromptComposer {

    private static final String INSTRUCTION =
            "Compress old crew memory into one compact running summary. Merge the new completed exchanges into "
                    + "the existing summary, preserve concrete names, numbers, decisions and outcomes, and drop "
                    + "repetition or conversational padding. Do not invent information. Reply with only the "
                    + "updated summary as plain text, at most "
                    + CompanionMemoryPolicy.summaryMaxChars() + " characters.";

    /** Returns the system instruction and user data for one kind-specific consolidation batch. */
    List<LlmMessage> compose(MemoryKind kind, String currentSummary, List<MemoryRecord> batch) {
        StringBuilder user = new StringBuilder();
        user.append("Memory kind: ").append(kind.name()).append('\n');
        user.append("Existing summary:\n")
                .append(currentSummary == null || currentSummary.isBlank() ? "(none)" : currentSummary.strip());
        user.append("\n\nCompleted exchanges to merge:\n");
        for (MemoryRecord record : batch) {
            user.append("[record ").append(record.timestamp()).append("]\n");
            for (MemoryEntry entry : record.entries()) {
                user.append('[').append(entry.source().name()).append("] ")
                        .append(entry.content()).append('\n');
            }
        }
        return List.of(
                LlmMessage.of(LlmMessageRole.SYSTEM, INSTRUCTION + " " + languageRule()),
                LlmMessage.of(LlmMessageRole.USER, user.toString()));
    }

    private static String languageRule() {
        Language language = AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(SystemSession.getInstance());
        return "The memory content is in " + language.displayName()
                + "; write the summary in " + language.displayName() + ".";
    }
}
