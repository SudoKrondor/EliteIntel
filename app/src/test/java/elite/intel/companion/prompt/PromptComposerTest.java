package elite.intel.companion.prompt;

import elite.intel.companion.memory.MemoryAvailabilitySnapshot;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.llm.LlmMessage;
import elite.intel.companion.model.llm.LlmMessageRole;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.companion.model.llm.PromptCacheProfile;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryProcessingState;
import elite.intel.companion.model.memory.MemorySource;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2 prompt assembly: message segmentation, cache profile by source, the static-prefix blocks
 * (topic enum, memory indexes), the timeline context block, and the current-input block. A stub
 * {@link SystemPromptText} keeps the test off the session/localization singletons.
 */
class PromptComposerTest {

    private static final String STATIC_MARKER = "<<STATIC RULES>>\n";

    private final PromptComposer composer = new PromptComposer(source -> STATIC_MARKER + source.name() + '\n');

    private static MemoryEntry entry(MemorySource source, ConversationTopic topic, String content) {
        return new MemoryEntry(Instant.now(), topic, source, content, MemoryProcessingState.PROCESSED);
    }

    private ComposedPrompt composeCommander(List<MemoryEntry> shortTerm, MemoryAvailabilitySnapshot indexes, String summary) {
        return composer.compose(
                ThoughtSource.COMMANDER, Urgency.NORMAL,
                ConversationTopic.NAVIGATION, ConversationTopic.PENDING,
                "set course to Sol",
                List.of(), List.of(),
                shortTerm, indexes, summary);
    }

    @Test
    void segmentsIntoSystemSystemUserAndPicksCommanderProfile() {
        ComposedPrompt prompt = composeCommander(List.of(),
                new MemoryAvailabilitySnapshot(0, 15, List.of()), null);

        List<LlmMessage> messages = prompt.messages();
        assertEquals(3, messages.size());
        assertEquals(LlmMessageRole.SYSTEM, messages.get(0).role());
        assertEquals(LlmMessageRole.SYSTEM, messages.get(1).role());
        assertEquals(LlmMessageRole.USER, messages.get(2).role());
        assertEquals(PromptCacheProfile.COMMANDER, prompt.profile());
        // The static block is taken from the injected owner, source-aware.
        assertTrue(messages.get(0).content().startsWith(STATIC_MARKER));
        assertTrue(messages.get(0).content().contains("COMMANDER"));
    }

    @Test
    void eventSourcePicksEventProfileAndSourceRules() {
        ComposedPrompt prompt = composer.compose(
                ThoughtSource.EVENT, Urgency.URGENT,
                ConversationTopic.COMBAT, ConversationTopic.COMBAT,
                "hostile interdiction",
                List.of(), List.of(),
                List.of(), new MemoryAvailabilitySnapshot(0, 15, List.of()), null);

        assertEquals(PromptCacheProfile.EVENT, prompt.profile());
        assertTrue(prompt.messages().get(0).content().contains("EVENT"));
    }

    @Test
    void topicEnumListsSelectableTopicsAndHidesSentinels() {
        String prefix = composeCommander(List.of(),
                new MemoryAvailabilitySnapshot(0, 15, List.of()), null).messages().get(0).content();

        assertTrue(prefix.contains("- navigation: " + ConversationTopic.NAVIGATION.description()));
        assertTrue(prefix.contains("- combat: " + ConversationTopic.COMBAT.description()));
        // Internal sentinels must never be offered to the LLM.
        assertFalse(prefix.contains("- pending"));
        assertFalse(prefix.contains("unresolved_commander_input"));
    }

    @Test
    void memoryIndexesReflectSnapshot() {
        MemoryAvailabilitySnapshot indexes = new MemoryAvailabilitySnapshot(
                7, 15, List.of(ConversationTopic.NAVIGATION, ConversationTopic.TRADE));
        String prefix = composeCommander(List.of(), indexes, "we left Sol heading rimward").messages().get(0).content();

        assertTrue(prefix.contains("### llm_memory"));
        assertTrue(prefix.contains("7 / 15 remembered items available."));
        assertTrue(prefix.contains("### Topic memory"));
        assertTrue(prefix.contains("- navigation: " + ConversationTopic.NAVIGATION.description()));
        assertTrue(prefix.contains("- trade: " + ConversationTopic.TRADE.description()));
        assertTrue(prefix.contains("### Long-term summary"));
        assertTrue(prefix.contains("we left Sol heading rimward"));
    }

    @Test
    void emptyTopicMemoryAndSummaryRenderPlaceholders() {
        String prefix = composeCommander(List.of(),
                new MemoryAvailabilitySnapshot(0, 15, List.of()), "  ").messages().get(0).content();

        assertTrue(prefix.contains("### Topic memory\n- none"));
        assertTrue(prefix.contains("### Long-term summary\nnone yet."));
    }

    @Test
    void contextBlockRendersTimelineLines() {
        List<MemoryEntry> shortTerm = List.of(
                entry(MemorySource.COMMANDER, ConversationTopic.NAVIGATION, "where are we"),
                entry(MemorySource.TOOL_RESULT, ConversationTopic.NAVIGATION, "in Sol"));
        String context = composeCommander(shortTerm,
                new MemoryAvailabilitySnapshot(0, 15, List.of()), null).messages().get(1).content();

        assertTrue(context.startsWith("## Session memory timeline"));
        assertTrue(context.contains("[COMMANDER][navigation][processed] where are we"));
        assertTrue(context.contains("[TOOL_RESULT][navigation][processed] in Sol"));
    }

    @Test
    void emptyTimelineRendersPlaceholder() {
        String context = composeCommander(List.of(),
                new MemoryAvailabilitySnapshot(0, 15, List.of()), null).messages().get(1).content();
        assertTrue(context.contains("(empty)"));
    }

    @Test
    void currentInputBlockCarriesSourceUrgencyTopicsAndContent() {
        String input = composeCommander(List.of(),
                new MemoryAvailabilitySnapshot(0, 15, List.of()), null).messages().get(2).content();

        assertTrue(input.contains("source: COMMANDER"));
        assertTrue(input.contains("urgency: normal"));
        assertTrue(input.contains("global topic: navigation"));
        assertTrue(input.contains("current topic: pending"));
        assertTrue(input.contains("content: set course to Sol"));
    }

    @Test
    void toolsConcatenateSelectedThenSystem() {
        LlmToolDefinition game = new LlmToolDefinition("set_course", "d", "", List.of());
        LlmToolDefinition system = new LlmToolDefinition("speak", "d", "", List.of());
        ComposedPrompt prompt = composer.compose(
                ThoughtSource.COMMANDER, Urgency.NORMAL,
                ConversationTopic.NAVIGATION, ConversationTopic.PENDING, "go",
                List.of(game), List.of(system),
                List.of(), new MemoryAvailabilitySnapshot(0, 15, List.of()), null);

        assertEquals(List.of(game, system), prompt.tools());
    }
}
