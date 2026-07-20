package elite.intel.vega.prompt;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import elite.intel.vega.clarify.PendingClarification;
import elite.intel.vega.llm.AnthropicLlmAdapter;
import elite.intel.vega.llm.GeminiLlmAdapter;
import elite.intel.vega.model.ThoughtSource;
import elite.intel.vega.model.llm.LlmMessage;
import elite.intel.vega.model.llm.LlmMessageRole;
import elite.intel.vega.model.llm.LlmRequest;
import elite.intel.vega.model.llm.LlmToolDefinition;
import elite.intel.vega.model.llm.PromptCacheProfile;
import elite.intel.vega.model.memory.MemoryRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptComposerTest {

    private static final String STATIC_MARKER = "<<STATIC RULES>>\n";
    private final PromptComposer composer = new PromptComposer(source -> STATIC_MARKER + source.name());

    private ComposedPrompt composeCommander(List<MemoryRecord> history) {
        return composer.compose(
                ThoughtSource.COMMANDER, "set course to Sol",
                List.of(), List.of(), history, List.of());
    }

    private static LlmMessage currentInput(ComposedPrompt prompt) {
        return prompt.messages().get(prompt.messages().size() - 1);
    }

    @Test
    void emptyHistoryProducesStablePrefixAndCurrentInput() {
        ComposedPrompt prompt = composeCommander(List.of());

        assertEquals(PromptCacheProfile.COMMANDER, prompt.profile());
        assertEquals(2, prompt.messages().size());
        assertEquals(LlmMessageRole.SYSTEM, prompt.messages().get(0).role());
        assertEquals(STATIC_MARKER + "COMMANDER", prompt.messages().get(0).content());
        assertEquals(LlmMessageRole.USER, currentInput(prompt).role());
        assertEquals("set course to Sol", currentInput(prompt).content());
    }

    @Test
    void dialogueRecordReplaysAsOneCompleteRolePair() {
        List<LlmMessage> messages = composeCommander(List.of(
                MemoryRecord.dialogue(Instant.EPOCH, "where are we", "we are in Sol"))).messages();

        assertEquals(4, messages.size());
        assertEquals(LlmMessageRole.USER, messages.get(1).role());
        assertEquals("where are we", messages.get(1).content());
        assertEquals(LlmMessageRole.ASSISTANT, messages.get(2).role());
        assertEquals("we are in Sol", messages.get(2).content());
    }

    @Test
    void eventRecordIsNotReplayedAsSyntheticChatHistory() {
        List<LlmMessage> messages = composeCommander(List.of(
                MemoryRecord.event(Instant.EPOCH, "alexandrite and void opals detected"))).messages();

        assertEquals(2, messages.size());
        assertTrue(messages.stream().map(LlmMessage::content)
                .noneMatch("alexandrite and void opals detected"::equals));
    }

    @Test
    void queryRecordReplaysAsCompletedCommanderCompanionPair() {
        List<LlmMessage> messages = composeCommander(List.of(MemoryRecord.query(
                Instant.EPOCH, "where are we", "in Sol")))
                .messages();

        assertEquals(4, messages.size());
        assertEquals(LlmMessageRole.USER, messages.get(1).role());
        assertEquals("where are we", messages.get(1).content());
        assertEquals(LlmMessageRole.ASSISTANT, messages.get(2).role());
        assertEquals("in Sol", messages.get(2).content());
        assertTrue(messages.get(2).toolCalls().isEmpty());
    }

    @Test
    void completedRecordsNeedNoSyntheticBoundaryRepair() {
        List<LlmMessage> messages = composeCommander(List.of(
                MemoryRecord.dialogue(Instant.EPOCH, "hello", "hello commander"),
                MemoryRecord.query(Instant.ofEpochSecond(1),
                        "cargo?", "empty"))).messages();

        assertEquals(List.of(
                        LlmMessageRole.SYSTEM,
                        LlmMessageRole.USER,
                        LlmMessageRole.ASSISTANT,
                        LlmMessageRole.USER,
                        LlmMessageRole.ASSISTANT,
                        LlmMessageRole.USER),
                messages.stream().map(LlmMessage::role).toList());
        assertTrue(messages.stream().map(LlmMessage::content).filter(java.util.Objects::nonNull)
                .noneMatch(content -> content.contains("<no_reply/>")));
        assertTrue(messages.stream().map(LlmMessage::content).filter(java.util.Objects::nonNull)
                .noneMatch(content -> content.contains("<processing/>")));
    }

    @Test
    void queryHistoryProducesAlternatingAnthropicAndGeminiTurns() {
        ComposedPrompt prompt = composeCommander(List.of(
                MemoryRecord.query(Instant.EPOCH, "cargo?", "empty")));
        LlmRequest request = new LlmRequest(
                "request", prompt.messages(), List.of(), PromptCacheProfile.COMMANDER);

        JsonArray anthropicMessages = JsonParser.parseString(
                        new AnthropicLlmAdapter().buildRequestBody(request))
                .getAsJsonObject().getAsJsonArray("messages");
        assertEquals(List.of("user", "assistant", "user"), roles(anthropicMessages));

        JsonArray geminiContents = JsonParser.parseString(
                        new GeminiLlmAdapter().buildRequestBody(request))
                .getAsJsonObject().getAsJsonArray("contents");
        assertEquals(List.of("user", "model", "user"), roles(geminiContents));
    }

    private static List<String> roles(JsonArray turns) {
        List<String> roles = new ArrayList<>(turns.size());
        turns.forEach(turn -> roles.add(turn.getAsJsonObject().get("role").getAsString()));
        return List.copyOf(roles);
    }

    @Test
    void savedTextRecordIsIgnoredBecauseItIsNotHistory() {
        List<LlmMessage> messages = composeCommander(List.of(
                MemoryRecord.savedText(Instant.EPOCH, "remember this"))).messages();

        assertEquals(2, messages.size());
    }

    @Test
    void factsEndTheSystemMessageWhilePendingClarificationStaysWithCurrentInput() {
        PendingClarification pending = new PendingClarification(
                "set_speed", "amount", "set speed", "By how much?", Instant.MAX);
        ComposedPrompt prompt = composer.compose(
                ThoughtSource.COMMANDER, "fifty percent",
                List.of(), List.of(), List.of(),
                List.of(new Fact("fuel is low <10%", "event")), pending);

        assertEquals(2, prompt.messages().size());
        String system = prompt.messages().getFirst().content();
        String current = currentInput(prompt).content();
        assertTrue(system.endsWith("<facts>\n"
                + "  <fact id=\"1\" source=\"event\">fuel is low &lt;10%</fact>\n"
                + "</facts>\n"));
        assertFalse(current.contains("<facts>"));
        assertTrue(current.contains("<pending_clarification>"));
        assertTrue(current.contains("<action_id>set_speed</action_id>"));
        assertTrue(current.contains("<commander_input>\nfifty percent"));
    }

    @Test
    void factsDoNotWrapTheCommanderInput() {
        ComposedPrompt prompt = composer.compose(
                ThoughtSource.COMMANDER, "set speed to fifty percent",
                List.of(), List.of(), List.of(), List.of(new Fact("in supercruise", "situation")));

        assertEquals("set speed to fifty percent", currentInput(prompt).content());
        assertTrue(prompt.messages().getFirst().content().endsWith("</facts>\n"));
    }

    @Test
    void eventSourceUsesNarrationProfileAndOnlySystemTools() {
        LlmToolDefinition speak = new LlmToolDefinition("speak", "d", "", List.of());
        ComposedPrompt prompt = composer.compose(
                ThoughtSource.EVENT, PromptXml.element("event_data", "fuel reserve critical"),
                List.of(new LlmToolDefinition("ignored", "d", "", List.of())),
                List.of(speak), List.of(MemoryRecord.dialogue(
                        Instant.EPOCH, "invented old state", "fuel is full")), List.of());

        assertEquals(PromptCacheProfile.NARRATION, prompt.profile());
        assertEquals(2, prompt.messages().size());
        assertEquals(STATIC_MARKER + "EVENT", prompt.messages().get(0).content());
        assertEquals(PromptXml.element("event_data", "fuel reserve critical"), currentInput(prompt).content());
        assertTrue(prompt.messages().stream().noneMatch(message -> message.content().contains("fuel is full")));
        assertEquals(List.of(speak), prompt.tools());
    }

    @Test
    void factSourceIdCannotBreakTheContextMarkup() {
        assertThrows(IllegalArgumentException.class, () -> new Fact("text", "event\" injected=\"true"));
    }

    @Test
    void selectedGameToolsPrecedeSystemTools() {
        LlmToolDefinition game = new LlmToolDefinition("set_course", "d", "", List.of());
        LlmToolDefinition system = new LlmToolDefinition("speak", "d", "", List.of());

        ComposedPrompt prompt = composer.compose(
                ThoughtSource.COMMANDER, "go", List.of(game), List.of(system), List.of(), List.of());

        assertEquals(List.of(game, system), prompt.tools());
    }
}
