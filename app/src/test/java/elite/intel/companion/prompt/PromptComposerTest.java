package elite.intel.companion.prompt;

import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.llm.LlmMessage;
import elite.intel.companion.model.llm.LlmMessageRole;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.companion.model.llm.PromptCacheProfile;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryImportance;
import elite.intel.companion.model.memory.MemorySource;
import elite.intel.companion.model.memory.ToolLink;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2 prompt assembly: message segmentation, cache profile by source, the static-prefix blocks
 * (topic enum), the role-based conversation history (including tool-call pair replay), and the current-input
 * block. A stub {@link SystemPromptText} keeps the test off the session/localization singletons.
 */
class PromptComposerTest {

    private static final String STATIC_MARKER = "<<STATIC RULES>>\n";

    private final PromptComposer composer = new PromptComposer(source -> STATIC_MARKER + source.name() + '\n');

    private static MemoryEntry entry(MemorySource source, ConversationTopic topic, String content) {
        return new MemoryEntry(Instant.now(), topic, source, content);
    }

    private static MemoryEntry call(String toolCallId, String toolName, String argsJson) {
        return new MemoryEntry(Instant.now(), ConversationTopic.NAVIGATION, MemorySource.COMPANION,
                "", MemoryImportance.LOW, null, null, ToolLink.call(toolCallId, toolName, argsJson));
    }

    private static MemoryEntry result(String toolCallId, String content) {
        return new MemoryEntry(Instant.now(), ConversationTopic.NAVIGATION, MemorySource.TOOL_RESULT,
                content, MemoryImportance.NORMAL, null, null, ToolLink.result(toolCallId));
    }

    private static long systemCount(List<LlmMessage> messages) {
        return messages.stream().filter(m -> m.role() == LlmMessageRole.SYSTEM).count();
    }

    private ComposedPrompt composeCommander(List<MemoryEntry> shortTerm) {
        return composer.compose(
                ThoughtSource.COMMANDER,
                "set course to Sol",
                List.of(), List.of(),
                shortTerm, List.of());
    }

    /** The current-input block is always the final message. */
    private static LlmMessage currentInput(ComposedPrompt prompt) {
        return prompt.messages().get(prompt.messages().size() - 1);
    }

    @Test
    void segmentsIntoSystemThenUserAndPicksCommanderProfile() {
        ComposedPrompt prompt = composeCommander(List.of());

        List<LlmMessage> messages = prompt.messages();
        // Empty history: just the cached system prefix and the current-input user message.
        assertEquals(2, messages.size());
        assertEquals(LlmMessageRole.SYSTEM, messages.get(0).role());
        assertEquals(LlmMessageRole.USER, messages.get(1).role());
        assertEquals(PromptCacheProfile.COMMANDER, prompt.profile());
        // The static block is taken from the injected owner, source-aware.
        assertTrue(messages.get(0).content().startsWith(STATIC_MARKER));
        assertTrue(messages.get(0).content().contains("COMMANDER"));
    }

    @Test
    void eventSourcePicksLeanPromptAndProfile() {
        LlmToolDefinition speak = new LlmToolDefinition("speak", "d", "", List.of());
        ComposedPrompt prompt = composer.compose(
                ThoughtSource.EVENT,
                PromptXml.element("event_data", "fuel reserve critical"),
                List.of(), List.of(speak),
                List.of(), List.of());

        assertEquals(PromptCacheProfile.NARRATION, prompt.profile());
        // 2 messages (empty history): the lean event-reaction static block and the current input.
        assertEquals(2, prompt.messages().size());
        String system = prompt.messages().get(0).content();
        assertTrue(system.startsWith(STATIC_MARKER));
        assertTrue(system.contains("EVENT"));
        // Lean: the commander-only stable-prefix sections are not stacked onto the reaction block.
        assertFalse(system.contains("Topics"));
        assertEquals(PromptXml.element("event_data", "fuel reserve critical"),
                currentInput(prompt).content());
        // Only the system functions are offered (a reactive event thought has no game tools).
        assertEquals(List.of(speak), prompt.tools());
    }

    @Test
    void eventSourceKeepsTaggedInputWhenAmbientContextExists() {
        ComposedPrompt prompt = composer.compose(
                ThoughtSource.EVENT,
                PromptXml.element("event_data", "surface scan: <alexandrite>"),
                List.of(), List.of(),
                List.of(entry(MemorySource.SYSTEM, ConversationTopic.SYSTEM, "diagnostic <note>")),
                List.of());

        String current = currentInput(prompt).content();
        assertTrue(current.contains("<context>"));
        assertTrue(current.contains("diagnostic &lt;note&gt;"));
        assertTrue(current.contains("<event_data>"));
        assertTrue(current.contains("surface scan: &lt;alexandrite&gt;"));
        assertFalse(current.contains("&lt;event_data&gt;"), "event data must remain a tag, not escaped text");
    }

    @Test
    void topicEnumListsSelectableTopicsAndHidesSentinels() {
        String prefix = composeCommander(List.of()).messages().get(0).content();

        assertTrue(prefix.contains("- navigation: " + ConversationTopic.NAVIGATION.description()));
        assertTrue(prefix.contains("- combat: " + ConversationTopic.COMBAT.description()));
        // Internal sentinels must never be offered to the LLM.
        assertFalse(prefix.contains("- pending"));
        assertFalse(prefix.contains("unresolved_commander_input"));
    }

    @Test
    void historyReplaysTimelineAsRolesWithoutTags() {
        List<MemoryEntry> shortTerm = List.of(
                entry(MemorySource.COMMANDER, ConversationTopic.NAVIGATION, "where are we"),
                entry(MemorySource.COMPANION, ConversationTopic.NAVIGATION, "we are in Sol"));
        List<LlmMessage> messages = composeCommander(shortTerm).messages();

        // system prefix, user (commander), assistant (companion), user (current input)
        assertEquals(4, messages.size());
        assertEquals(LlmMessageRole.USER, messages.get(1).role());
        assertEquals("where are we", messages.get(1).content());
        assertEquals(LlmMessageRole.ASSISTANT, messages.get(2).role());
        assertEquals("we are in Sol", messages.get(2).content());
        // The role now carries the speaker: history lines no longer carry [speaker]/[topic] tags.
        assertFalse(messages.get(1).content().contains("["));
        assertFalse(messages.get(2).content().contains("["));
    }

    @Test
    void replaysToolCallAndResultAsAProtocolPair() {
        List<LlmMessage> messages = composeCommander(List.of(
                call("tc1", "get_location", "{}"),
                result("tc1", "in Sol"))).messages();

        // system, assistant(tool_calls), tool(result), user
        assertEquals(4, messages.size());
        LlmMessage assistant = messages.get(1);
        assertEquals(LlmMessageRole.ASSISTANT, assistant.role());
        assertEquals(1, assistant.toolCalls().size());
        assertEquals("get_location", assistant.toolCalls().get(0).name());
        assertEquals("tc1", assistant.toolCalls().get(0).id());
        LlmMessage tool = messages.get(2);
        assertEquals(LlmMessageRole.TOOL, tool.role());
        assertEquals("tc1", tool.toolCallId());
        assertEquals("in Sol", tool.content());
    }

    @Test
    void pairsACallWithItsLaterNonAdjacentResult() {
        // The result is written by a later narration thought, so a commander turn can sit between call and result.
        List<LlmMessage> messages = composeCommander(List.of(
                call("tc2", "deploy_hardpoints", "{}"),
                entry(MemorySource.COMMANDER, ConversationTopic.NAVIGATION, "status"),
                result("tc2", "hardpoints deployed"))).messages();

        // system, assistant(tool_calls), tool(result) pulled up next to its call, the commander "status" history
        // turn, a no-reply assistant boundary to keep role alternation, then the distinct current-input user
        // message (never coalesced into the history).
        assertEquals(6, messages.size());
        assertEquals(LlmMessageRole.ASSISTANT, messages.get(1).role());
        assertEquals(LlmMessageRole.TOOL, messages.get(2).role());
        assertEquals("hardpoints deployed", messages.get(2).content());
        assertEquals(LlmMessageRole.USER, messages.get(3).role());
        assertEquals("status", messages.get(3).content());
        assertEquals(LlmMessageRole.ASSISTANT, messages.get(4).role());
        assertEquals("<no_reply/>", messages.get(4).content());
        assertEquals(LlmMessageRole.USER, messages.get(5).role());
        assertTrue(messages.get(5).content().contains("set course to Sol"));
    }

    @Test
    void coalescesConsecutiveSameRoleHistoryTurns() {
        // Two commander turns back to back in history (a silent reply between them) merge into one user message;
        // the current turn stays a distinct final user message, not fused into that block.
        List<LlmMessage> messages = composeCommander(List.of(
                entry(MemorySource.COMMANDER, ConversationTopic.NAVIGATION, "hey"),
                entry(MemorySource.COMMANDER, ConversationTopic.NAVIGATION, "you there?"))).messages();

        // system, both commander history turns merged into one user, a no-reply assistant boundary to keep
        // alternation, then the separate current-input user.
        assertEquals(4, messages.size());
        assertEquals(LlmMessageRole.USER, messages.get(1).role());
        assertTrue(messages.get(1).content().contains("hey"));
        assertTrue(messages.get(1).content().contains("you there?"));
        assertFalse(messages.get(1).content().contains("set course to Sol"), "the current turn stays a separate message");
        assertEquals(LlmMessageRole.ASSISTANT, messages.get(2).role());
        assertEquals("<no_reply/>", messages.get(2).content());
        assertEquals(LlmMessageRole.USER, messages.get(3).role());
        assertTrue(messages.get(3).content().contains("set course to Sol"));
    }

    @Test
    void synthesizesResultForAnUnmatchedToolCall() {
        List<LlmMessage> messages = composeCommander(List.of(call("tc9", "retract_gear", "{}"))).messages();

        // system, assistant(tool_calls), tool(synthesized), user - the pair stays protocol-valid even with no result.
        assertEquals(4, messages.size());
        assertEquals(LlmMessageRole.TOOL, messages.get(2).role());
        assertEquals("tc9", messages.get(2).toolCallId());
    }

    @Test
    void currentInputIsThePlainCommanderTurn() {
        String input = currentInput(composeCommander(List.of())).content();

        // The current turn is just the commander's words: no envelope and no injected topic hint. Its recency
        // in the message list marks it as the current turn; the model classifies the topic itself.
        assertEquals("set course to Sol", input);
        assertFalse(input.contains("current topic"));
        assertFalse(input.contains("## Current input"));
    }

    @Test
    void factsAndAmbientNotesAreInFinalUserContextNotSystemMessages() {
        ComposedPrompt prompt = composer.compose(
                ThoughtSource.COMMANDER,
                "where are we",
                List.of(), List.of(),
                List.of(entry(MemorySource.SYSTEM, ConversationTopic.SYSTEM, "diagnostic note")),
                List.of(new Fact("current system Sol", "memory")));
        List<LlmMessage> messages = prompt.messages();

        assertEquals(2, messages.size());
        assertEquals(1, systemCount(messages), "only the leading system message is allowed");
        assertEquals(LlmMessageRole.SYSTEM, messages.get(0).role());
        assertEquals(LlmMessageRole.USER, messages.get(1).role());
        String current = messages.get(1).content();
        assertTrue(current.contains("<context>"));
        assertTrue(current.contains("<facts>"));
        assertTrue(current.contains("current system Sol"));
        assertTrue(current.contains("<ambient_context>"));
        assertTrue(current.contains("diagnostic note"));
        assertTrue(current.contains("<commander_input>"));
        assertTrue(current.contains("where are we"));
    }

    @Test
    void eventStimulusReplaysAsUserTurn() {
        // A reactive event stimulus is world data on the user channel: it replays as a tagged user turn in the
        // history (not an ambient note), so its spoken reply reads as a proper reaction without making the raw
        // payload look like commander speech.
        List<LlmMessage> messages = composeCommander(List.of(
                entry(MemorySource.EVENT, ConversationTopic.EXPLORATION, "signals found on the ring"),
                entry(MemorySource.COMPANION, ConversationTopic.EXPLORATION, "alexandrite and void opals, Commander")))
                .messages();

        assertEquals(4, messages.size());
        assertEquals(1, systemCount(messages), "an event turn must not create a mid-dialogue system message");
        assertEquals(LlmMessageRole.USER, messages.get(1).role());
        assertEquals(PromptXml.element("event_data", "signals found on the ring"), messages.get(1).content());
        assertEquals(LlmMessageRole.ASSISTANT, messages.get(2).role());
        assertEquals("alexandrite and void opals, Commander", messages.get(2).content());
        assertEquals(LlmMessageRole.USER, messages.get(3).role(), "then the current commander input");
    }

    @Test
    void boundaryRepliesReplayAsAssistant() {
        // A turn that drew no reply records a <no_reply/> boundary as the companion's (assistant-side) omitted
        // reply - a COMPANION entry - so it replays as a plain assistant message that keeps role alternation,
        // with no SYSTEM-to-assistant indirection.
        List<LlmMessage> messages = composeCommander(List.of(
                entry(MemorySource.COMMANDER, ConversationTopic.NAVIGATION, "status"),
                entry(MemorySource.COMPANION, ConversationTopic.NAVIGATION, "<no_reply/>"))).messages();

        assertEquals(4, messages.size());
        assertEquals(1, systemCount(messages), "a boundary reply must not create a mid-dialogue system message");
        assertEquals(LlmMessageRole.USER, messages.get(1).role());
        assertEquals(LlmMessageRole.ASSISTANT, messages.get(2).role());
        assertEquals("<no_reply/>", messages.get(2).content());
        assertEquals(LlmMessageRole.USER, messages.get(3).role());
    }

    @Test
    void toolsConcatenateSelectedThenSystem() {
        LlmToolDefinition game = new LlmToolDefinition("set_course", "d", "", List.of());
        LlmToolDefinition system = new LlmToolDefinition("speak", "d", "", List.of());
        ComposedPrompt prompt = composer.compose(
                ThoughtSource.COMMANDER, "go",
                List.of(game), List.of(system),
                List.of(), List.of());

        assertEquals(List.of(game, system), prompt.tools());
    }
}
