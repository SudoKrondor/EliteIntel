package elite.intel.companion.prompt;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemorySource;
import elite.intel.companion.model.memory.ToolLink;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.llm.LlmMessage;
import elite.intel.companion.model.llm.LlmMessageRole;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.companion.model.llm.LlmToolInvocation;
import elite.intel.companion.model.llm.PromptCacheProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dumb stacker of {@code messages + tools}. It does not decide which tools are allowed, which
 * commands are relevant, or how to describe a tool; it receives already-prepared data and assembles
 * the OpenAI/Mistral-compatible prompt (see COMPANION_ARCHITECTURE.md §2.10).
 * <p>
 * Cache-friendly ordering: the stable narrative + topic enum head the system message, and the
 * per-turn conversation history, remembered-fact candidates, and current input are separate later
 * messages so the cached prefix survives across turns.
 * <p>
 * Conversation history is replayed as native role-alternating messages (commander -> {@code user}, the
 * companion's own words -> {@code assistant}, a model tool-call -> {@code assistant(tool_calls)} immediately
 * followed by its {@code tool} result) rather than flattened into one system block, so the model reads the
 * dialogue in the role structure it was aligned on (better coreference/turn-taking). Ambient timeline entries
 * that are not dialogue turns (events, system notes) are replayed as inline {@code system} notes.
 */
public final class PromptComposer {

    private final SystemPromptText systemPrompt;

    /** Production constructor: uses the real {@link CompanionSystemPromptPart} owner. */
    public PromptComposer() {
        this(new CompanionSystemPromptPart());
    }

    /** Injectable constructor for tests (avoids the session/localization singletons). */
    PromptComposer(SystemPromptText systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    /**
     * Assembles the initial messages and tool list for one thought turn.
     *
     * @param source           thought source (selects the static prefix and cache profile)
     * @param currentInput     the current commander reply or event summary
     * @param selectedTools    Reducer-selected game/query tools
     * @param systemTools      system function tools for this source
     * @param shortTerm        short-term memory timeline replayed as role-based history
     * @param memoryCandidates pre-selected clean answer facts (text + provenance) to inline as a {@code <facts>} block (may be empty)
     */
    public ComposedPrompt compose(
            ThoughtSource source,
            String currentInput,
            List<LlmToolDefinition> selectedTools,
            List<LlmToolDefinition> systemTools,
            List<MemoryEntry> shortTerm,
            List<MemoryFactCandidates.Fact> memoryCandidates
    ) {
        return switch (source) {
            case COMMANDER -> composeCommander(source, currentInput,
                    selectedTools, systemTools, shortTerm, memoryCandidates);
            case NARRATION -> composeNarration(source, currentInput, systemTools, shortTerm);
            // EVENT thoughts are memory-only (see EventThought); they never reach here.
            case EVENT -> throw new IllegalStateException("EVENT thoughts do not compose a prompt");
        };
    }

    /**
     * Full consciousness prompt: stable prefix (rules + topic enum), the role-based conversation history, the
     * answer-fact candidates, the lean current-input message, the reduced game tools plus system functions, and
     * the COMMANDER cache profile.
     */
    private ComposedPrompt composeCommander(
            ThoughtSource source, String currentInput,
            List<LlmToolDefinition> selectedTools, List<LlmToolDefinition> systemTools,
            List<MemoryEntry> shortTerm,
            List<MemoryFactCandidates.Fact> memoryCandidates) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(LlmMessage.of(LlmMessageRole.SYSTEM, buildStablePrefix(source)));
        messages.addAll(coalesceHistory(buildHistoryMessages(shortTerm)));
        // Per-turn clean answer facts (kept out of the cached prefix, like the history). Omitted when empty.
        if (memoryCandidates != null && !memoryCandidates.isEmpty()) {
            messages.add(LlmMessage.of(LlmMessageRole.SYSTEM, buildCandidatesBlock(memoryCandidates)));
        }
        // The current turn stays a distinct final user message - never coalesced into the history - so it is
        // never fused with a preceding (e.g. silently-answered) commander turn into one ambiguous message.
        messages.add(LlmMessage.of(LlmMessageRole.USER, buildCurrentInput(currentInput)));

        // Game/query tools first, then system functions; both already chosen upstream.
        List<LlmToolDefinition> tools = new ArrayList<>(selectedTools);
        tools.addAll(systemTools);

        return new ComposedPrompt(List.copyOf(messages), List.copyOf(tools), PromptCacheProfile.COMMANDER);
    }

    /**
     * Lean narration prompt: the narration static block only (no topic enum, no memory indexes, no safety -
     * a narration thought has only speak), the role-based conversation history for continuity, the sensor
     * data as the current input, the system tools, and its own NARRATION cache profile so it never shares the
     * commander prefix. {@code selectedTools} does not apply here.
     */
    private ComposedPrompt composeNarration(
            ThoughtSource source, String currentInput,
            List<LlmToolDefinition> systemTools, List<MemoryEntry> shortTerm) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(LlmMessage.of(LlmMessageRole.SYSTEM, systemPrompt.staticRules(source)));
        messages.addAll(coalesceHistory(buildHistoryMessages(shortTerm)));
        messages.add(LlmMessage.of(LlmMessageRole.USER, buildCurrentInput(currentInput)));

        return new ComposedPrompt(List.copyOf(messages), List.copyOf(systemTools), PromptCacheProfile.NARRATION);
    }

    /** Stable narrative + topic enum (the cached prefix). Remembered facts are inlined per-turn, not here. */
    private String buildStablePrefix(ThoughtSource source) {
        StringBuilder sb = new StringBuilder();
        sb.append(systemPrompt.staticRules(source));
        appendTopics(sb);
        return sb.toString();
    }

    /** Full selectable topic enum; the model needs the valid values for the classify_turn topic parameter. */
    private void appendTopics(StringBuilder sb) {
        PromptSections.heading(sb, "Topics");
        sb.append("Valid values for the classify_turn topic parameter:\n");
        for (ConversationTopic topic : ConversationTopic.values()) {
            if (topic.selectable()) {
                sb.append("- ").append(topic.id()).append(": ").append(topic.description()).append('\n');
            }
        }
        // The topic is classified fresh each turn from the turn's own content; a short continuation with no
        // topic of its own inherits the topic of what it continues, read from the conversation above (no
        // current-topic value is injected into the turn any more).
        sb.append("Classify every turn under the topic its content fits. If the turn is a short continuation "
                + "with no topic of its own (\"and then?\", \"yeah\"), use the conversation above to place it "
                + "under the topic it continues.\n");
    }

    /** Wire content of a synthesized tool result when a recorded tool-call left no voiced/textual outcome. */
    private static final String NO_TEXTUAL_RESULT = "(no textual result)";

    /**
     * Per-turn conversation history (kept out of the cached prefix): the short-term timeline replayed as native
     * role-alternating messages instead of one flattened block, so the model reads the dialogue in the role
     * structure it was aligned on. Durable facts that aged out of short-term are not replayed here - the
     * relevant ones are surfaced as an inline {@code <facts>} block (see {@link #buildCandidatesBlock}).
     * <p>
     * Mapping: {@code COMMANDER -> user}; the companion's own words {@code COMPANION -> assistant}; a recorded
     * model tool-call ({@code COMPANION} carrying a {@link ToolLink.Kind#CALL}) {@code -> assistant(tool_calls)}
     * immediately followed by its {@code tool} result (matched by tool-call id, since the result is written by a
     * later narration thought and may not be adjacent in the timeline; a missing result is synthesized so the
     * pair stays protocol-valid). Ambient entries that are not dialogue turns (events, system notes, an unlinked
     * legacy tool marker) become inline {@code system} notes.
     */
    private List<LlmMessage> buildHistoryMessages(List<MemoryEntry> shortTerm) {
        List<LlmMessage> out = new ArrayList<>();
        if (shortTerm == null || shortTerm.isEmpty()) {
            return out;
        }
        // Index tool results by their correlation id: a CALL pulls its RESULT to sit right after it, because the
        // result is written when the narration thought runs and may land later in the timeline than its call.
        Map<String, MemoryEntry> resultsById = new HashMap<>();
        for (MemoryEntry entry : shortTerm) {
            ToolLink link = entry.toolLink();
            if (entry.source() == MemorySource.TOOL_RESULT && link != null && link.isResult()) {
                resultsById.put(link.toolCallId(), entry);
            }
        }
        for (MemoryEntry entry : shortTerm) {
            ToolLink link = entry.toolLink();
            switch (entry.source()) {
                case COMMANDER -> out.add(LlmMessage.of(LlmMessageRole.USER, entry.content()));
                case COMPANION -> {
                    if (link != null && link.isCall()) {
                        out.add(LlmMessage.assistantToolCalls(List.of(invocationOf(link))));
                        MemoryEntry result = resultsById.get(link.toolCallId());
                        out.add(LlmMessage.toolResult(link.toolCallId(),
                                result != null ? result.content() : NO_TEXTUAL_RESULT));
                    } else {
                        out.add(LlmMessage.of(LlmMessageRole.ASSISTANT, entry.content()));
                    }
                }
                // A linked tool result is emitted right after its call above; skip it here (a bare tool message
                // with no preceding assistant tool-call is not protocol-valid, so an orphan result is dropped).
                // An unlinked legacy marker falls through to the ambient system note.
                case TOOL_RESULT -> {
                    if (link == null || !link.isResult()) {
                        out.add(LlmMessage.of(LlmMessageRole.SYSTEM, "(" + entry.content() + ")"));
                    }
                }
                case EVENT, SYSTEM -> out.add(LlmMessage.of(LlmMessageRole.SYSTEM, entry.content()));
            }
        }
        return out;
    }

    /**
     * Merges consecutive same-role plain messages <em>within the history</em> into one, so the transcript keeps
     * clean {@code user}/{@code assistant} alternation. Adjacent same-role history messages arise when a commander
     * turn drew a silent reply or two companion lines land back to back; a wall of same-role messages degrades
     * small-model turn-taking and is rejected by strict-alternation providers (Anthropic, Gemini). Applied only
     * to the history list - never the cached system prefix or the current-input message, which are assembled
     * around it - so the cache prefix and the distinct current turn are preserved. A message carrying tool-calls
     * or a tool result is a boundary and is never merged.
     */
    private static List<LlmMessage> coalesceHistory(List<LlmMessage> history) {
        List<LlmMessage> out = new ArrayList<>();
        for (LlmMessage m : history) {
            LlmMessage last = out.isEmpty() ? null : out.get(out.size() - 1);
            if (last != null && isPlain(last) && isPlain(m) && last.role() == m.role()) {
                out.set(out.size() - 1, LlmMessage.of(m.role(), last.content() + "\n" + m.content()));
            } else {
                out.add(m);
            }
        }
        return out;
    }

    /** A plain text message (no tool-calls, no tool result) - the only kind that may be merged with its neighbour. */
    private static boolean isPlain(LlmMessage m) {
        return m.content() != null && m.toolCalls().isEmpty() && m.toolCallId() == null;
    }

    /** Reconstructs the assistant tool-call invocation from a recorded {@link ToolLink.Kind#CALL} link. */
    private static LlmToolInvocation invocationOf(ToolLink link) {
        JsonObject args = new JsonObject();
        String json = link.argumentsJson();
        if (json != null && !json.isBlank()) {
            args = JsonParser.parseString(json).getAsJsonObject();
        }
        return new LlmToolInvocation(link.toolCallId(), link.toolName(), args);
    }

    /**
     * Per-turn remembered-fact context: data only - a single {@code <facts>} XML element whose children carry an
     * {@code id} and a {@code source} attribute (provenance: {@code "event"} past occurrence / {@code "commander"}
     * told to you). No heading and no usage instruction: the XML self-delineates the content, and how to use these
     * facts is owned once by the Function-calling rules, not duplicated per turn. XML with attributes tested best
     * for delineating provided context (OpenAI long-context guidance). Kept out of the cached prefix (it changes
     * every turn); only built when non-empty.
     */
    private String buildCandidatesBlock(List<MemoryFactCandidates.Fact> memoryCandidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("<facts>\n");
        int id = 1;
        for (MemoryFactCandidates.Fact fact : memoryCandidates) {
            sb.append("  <fact id=\"").append(id++).append("\" source=\"").append(fact.source()).append("\">")
                    .append(fact.text()).append("</fact>\n");
        }
        sb.append("</facts>\n");
        return sb.toString();
    }

    /**
     * The current turn as a plain {@code user} message: the commander's own words, nothing else. Its recency in
     * the message list is what marks it as the current turn (OpenAI multi-turn guidance), so no envelope or
     * per-turn metadata (source/urgency/topic) is added - that only duplicated the role or, mixed into the
     * words, read as if the commander had said it. The turn's topic is classified by the model from the words
     * and the conversation above (see the Topics section), not injected here.
     */
    private String buildCurrentInput(String currentInput) {
        return currentInput == null ? "" : currentInput;
    }
}
