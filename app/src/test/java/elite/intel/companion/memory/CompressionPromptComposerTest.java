package elite.intel.companion.memory;

import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.ai.brain.i18n.PromptLocalizations;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.llm.LlmMessage;
import elite.intel.companion.model.llm.LlmMessageRole;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemorySource;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the compression message flow: a system instruction stating the size cap, and a user block with
 * the existing summary (or "(none)") and the buffered entries tagged by source and topic.
 */
class CompressionPromptComposerTest {

    private final CompressionPromptComposer composer = new CompressionPromptComposer();

    private static MemoryEntry entry(ConversationTopic topic, String content) {
        return new MemoryEntry(Instant.now(), topic, MemorySource.EVENT, content);
    }

    /** Commander's language name, resolved exactly as production does, so the test holds in any environment. */
    private static String resolvedLanguageName() {
        Language language = AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(SystemSession.getInstance());
        return PromptLocalizations.rulesFor(language).languageName();
    }

    @Test
    void buildsSystemInstructionAndUserBlockWithEntries() {
        List<LlmMessage> messages = composer.compose("known so far",
                List.of(entry(ConversationTopic.MINING, "found platinum"),
                        entry(ConversationTopic.NAVIGATION, "jumped to Sol")));

        assertEquals(2, messages.size());

        LlmMessage system = messages.get(0);
        assertEquals(LlmMessageRole.SYSTEM, system.role());
        assertTrue(system.content().contains(String.valueOf(CompanionMemoryLimits.SUMMARY_MAX_CHARS)),
                "instruction must state the size cap");
        // Instruction stays English but names the commander's language and binds the summary to it.
        assertTrue(system.content().contains("write the summary in " + resolvedLanguageName()),
                "instruction must bind the summary to the commander's language");

        LlmMessage user = messages.get(1);
        assertEquals(LlmMessageRole.USER, user.role());
        assertTrue(user.content().contains("known so far"));
        assertTrue(user.content().contains("[EVENT][mining][normal] found platinum"));
        assertTrue(user.content().contains("[EVENT][navigation][normal] jumped to Sol"));
    }

    @Test
    void rendersNoneWhenSummaryEmpty() {
        List<LlmMessage> messages = composer.compose("", List.of(entry(ConversationTopic.TRADE, "sold cargo")));
        assertTrue(messages.get(1).content().contains("Existing summary:\n(none)"));
    }
}
