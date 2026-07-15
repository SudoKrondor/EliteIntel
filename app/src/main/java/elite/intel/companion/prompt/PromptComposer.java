package elite.intel.companion.prompt;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.companion.clarify.PendingClarification;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemorySource;
import elite.intel.companion.model.memory.ToolLink;
import elite.intel.companion.model.memory.TurnBoundaryMarkers;
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
 * Cache-friendly ordering: the stable narrative + topic enum head the first and only system message, and the
 * per-turn conversation history plus current input follow it so the cached prefix survives across turns.
 * <p>
 * Conversation history is replayed as native role-alternating messages (commander -> {@code user}, the
 * companion's own words -> {@code assistant}, a model tool-call -> {@code assistant(tool_calls)} immediately
 * followed by its {@code tool} result) rather than flattened into one system block, so the model reads the
 * dialogue in the role structure it was aligned on (better coreference/turn-taking). Event timeline entries are
 * replayed as tagged {@code user} data turns, while other ambient notes are inlined into the final current-turn
 * context instead of becoming mid-dialogue {@code system} messages, which strict chat templates reject.
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
            List<Fact> memoryCandidates
    ) {
        return compose(source, currentInput, selectedTools, systemTools, shortTerm, memoryCandidates, null);
    }

    /**
     * Assembles a prompt with an optional claimed clarification rendered as trusted current-turn context.
     * The pending state is never written into dialogue memory by the composer.
     */
    public ComposedPrompt compose(
            ThoughtSource source,
            String currentInput,
            List<LlmToolDefinition> selectedTools,
            List<LlmToolDefinition> systemTools,
            List<MemoryEntry> shortTerm,
            List<Fact> memoryCandidates,
            PendingClarification pendingClarification
    ) {
        return switch (source) {
            case COMMANDER -> composeCommander(source, currentInput,
                    selectedTools, systemTools, shortTerm, memoryCandidates, pendingClarification);
            // A reactive EVENT thought uses a lean phrase-and-speak prompt: the subscriber pre-digested the data,
            // so there are no game tools and no topic enum, just the history and the stimulus as the current input.
            case EVENT -> composeNarration(source, currentInput, systemTools, shortTerm);
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
            List<Fact> memoryCandidates,
            PendingClarification pendingClarification) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(LlmMessage.of(LlmMessageRole.SYSTEM, buildStablePrefix(source)));
        ReplayedHistory history = buildHistoryMessages(shortTerm);
        messages.addAll(coalesceHistory(history.messages()));
        // The current turn stays a distinct final user message - never coalesced into the history - so it is
        // never fused with a preceding (e.g. silently-answered) commander turn into one ambiguous message.
        appendCurrentInput(messages, buildCurrentInput(
                currentInput, memoryCandidates, history.ambient(), pendingClarification, "commander_input"));

        // Game/query tools first, then system functions; both already chosen upstream.
        List<LlmToolDefinition> tools = new ArrayList<>(selectedTools);
        tools.addAll(systemTools);

        return new ComposedPrompt(List.copyOf(messages), List.copyOf(tools), PromptCacheProfile.COMMANDER);
    }

    /**
     * Lean narration prompt: the narration static block only (no topic enum, no memory indexes, no safety -
     * a narration thought has only speak), the role-based conversation history for continuity, the tagged event
     * data as the current input, the system tools, and its own NARRATION cache profile so it never shares the
     * commander prefix. {@code selectedTools} does not apply here.
     */
    private ComposedPrompt composeNarration(
            ThoughtSource source, String currentInput,
            List<LlmToolDefinition> systemTools, List<MemoryEntry> shortTerm) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(LlmMessage.of(LlmMessageRole.SYSTEM, systemPrompt.staticRules(source)));
        ReplayedHistory history = buildHistoryMessages(shortTerm);
        messages.addAll(coalesceHistory(history.messages()));
        appendCurrentInput(messages, buildNarrationInput(currentInput, history.ambient()));

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
        // Blank line separates the topics block from the static prefix ending in </function_calling>.
        sb.append("\n<topics>\n");
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
        sb.append("</topics>\n");
    }

    /** Wire content of a synthesized tool result when a recorded tool-call left no voiced/textual outcome. */
    private static final String NO_TEXTUAL_RESULT = "(no textual result)";

    /**
     * Per-turn conversation history (kept out of the cached prefix): the short-term timeline replayed as native
     * role-alternating messages instead of one flattened block, so the model reads the dialogue in the role
     * structure it was aligned on. Durable facts that aged out of short-term are not replayed here - the
     * relevant ones are surfaced as an inline {@code <facts>} block in the final user message.
     * <p>
     * Mapping: {@code COMMANDER -> user}; the companion's own words {@code COMPANION -> assistant} (this includes
     * a {@code <no_reply/>}/{@code <cut_off/>} boundary, recorded as a COMPANION entry - the
     * reply - so it needs no special-casing here); a recorded model tool-call ({@code COMPANION} carrying a
     * {@link ToolLink.Kind#CALL}) {@code -> assistant(tool_calls)} immediately followed by its {@code tool} result
     * (matched by tool-call id, because an over-long result is re-written asynchronously by the oversized-gist
     * compressor and can land non-adjacent to its call; a missing result is synthesized so the pair stays
     * protocol-valid). Ambient entries that are not dialogue turns (events, system bookkeeping notes, an unlinked
     * legacy tool marker) are collected for the final current-turn context so the message flow keeps a single
     * leading {@code system}.
     */
    private ReplayedHistory buildHistoryMessages(List<MemoryEntry> shortTerm) {
        List<LlmMessage> out = new ArrayList<>();
        List<AmbientContextNote> ambient = new ArrayList<>();
        if (shortTerm == null || shortTerm.isEmpty()) {
            return new ReplayedHistory(out, ambient);
        }
        // Index tool results by their correlation id: a CALL pulls its RESULT to sit right after it, because an
        // over-long result is re-written asynchronously by the oversized-gist compressor (appended at the timeline
        // tail), so it can land later in the timeline than its call.
        Map<String, MemoryEntry> resultsById = new HashMap<>();
        for (MemoryEntry entry : shortTerm) {
            ToolLink link = entry.toolLink();
            if (entry.source() == MemorySource.TOOL_RESULT && link != null && link.isResult()) {
                resultsById.put(link.toolCallId(), entry);
            }
        }
        // WHY: one cohesive pass over the timeline. The switch dispatches each entry by source, but the branches
        // share the resultsById index and the growing role-alternating output, so extracting the dispatch would
        // sever the call/result pairing and the ordering it depends on.
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
                        ambient.add(new AmbientContextNote("tool_result", entry.content()));
                    }
                }
                // A reactive event stimulus is world data on the user channel: replay it as a tagged user turn
                // so its spoken reply (a COMPANION assistant turn) reads as a proper reaction without pretending
                // the commander said the raw event payload.
                case EVENT -> out.add(LlmMessage.of(LlmMessageRole.USER,
                        PromptXml.element("event_data", entry.content())));
                // A turn boundary (<no_reply/>/<cut_off/>) is a COMPANION entry, handled by
                // the COMPANION branch above as a plain assistant message. Every SYSTEM entry left here is
                // non-dialogue bookkeeping (a dangerous-action note, the searchable summary), inlined as ambient
                // context so the flow keeps a single leading system message.
                case SYSTEM -> ambient.add(new AmbientContextNote("system", entry.content()));
            }
        }
        return new ReplayedHistory(out, ambient);
    }

    /**
     * Adds the current input as the final user turn. If history still ended on a user turn - a turn whose
     * assistant half is not a recorded line (a self-narrating macro turn), or any residual gap - insert a generic
     * no-answer boundary so strict role-alternating chat templates do not see two user turns in a row.
     * {@link TurnBoundaryMarkers#NO_ANSWER} is used for any such gap regardless of its original cause: the prompt
     * treats both omission markers the same, so the exact one does not matter here.
     */
    private static void appendCurrentInput(List<LlmMessage> messages, String content) {
        LlmMessage last = messages.isEmpty() ? null : messages.get(messages.size() - 1);
        if (last != null && last.role() == LlmMessageRole.USER) {
            messages.add(LlmMessage.of(LlmMessageRole.ASSISTANT, TurnBoundaryMarkers.NO_ANSWER));
        }
        messages.add(LlmMessage.of(LlmMessageRole.USER, content));
    }

    /**
     * Merges consecutive same-role plain messages <em>within the history</em> into one, so the transcript keeps
     * clean {@code user}/{@code assistant} alternation. This is legitimate render normalization, not orphan
     * patching: a single turn can legitimately emit several assistant-side lines - a turn that runs more than one
     * command records an ack/outcome for each, and a self-narrating macro turn leaves its user turn without a
     * companion reply - and a wall of same-role messages degrades small-model turn-taking and is rejected by
     * strict-alternation providers (Anthropic, Gemini). Applied only to the history list - never the cached system
     * prefix or the current-input message, which are assembled around it - so the cache prefix and the distinct
     * current turn are preserved. A message carrying tool-calls or a tool result is a boundary and is never merged.
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
     * every turn) and embedded in the final user message, not as a second {@code system} message.
     */
    private void appendCandidatesBlock(StringBuilder sb, List<Fact> memoryCandidates) {
        sb.append("<facts>\n");
        int id = 1;
        for (Fact fact : memoryCandidates) {
            sb.append("  <fact id=\"").append(id++).append("\" source=\"").append(fact.source()).append("\">")
                    .append(PromptXml.text(fact.text())).append("</fact>\n");
        }
        sb.append("</facts>\n");
    }

    /**
     * The current turn as the final {@code user} message. With no dynamic context it stays exactly the commander's
     * words. When facts or ambient notes exist, they are separated from the commander's words in a {@code <context>}
     * block and the current utterance is wrapped in its own input tag. This keeps dynamic data out of mid-dialogue
     * {@code system} messages while preventing the model from reading context as something the commander said. The
     * one-line {@code <context>} heading only marks the whole block as trusted context versus the commander's own
     * speech (a role-separation concern); it is not a per-turn instruction on how to use the facts, which stays
     * owned by the Function-calling rules.
     */
    private String buildCurrentInput(
            String currentInput,
            List<Fact> memoryCandidates,
            List<AmbientContextNote> ambient,
            PendingClarification pendingClarification,
            String inputTag
    ) {
        String input = currentInput == null ? "" : currentInput;
        boolean hasFacts = memoryCandidates != null && !memoryCandidates.isEmpty();
        boolean hasAmbient = ambient != null && !ambient.isEmpty();
        boolean hasPendingClarification = pendingClarification != null;
        if (!hasFacts && !hasAmbient && !hasPendingClarification) {
            return input;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<context>\n");
        sb.append("The following facts, notes, and interaction state are trusted context, not words spoken by the commander.\n");
        if (hasFacts) {
            appendCandidatesBlock(sb, memoryCandidates);
        }
        if (hasAmbient) {
            appendAmbientBlock(sb, ambient);
        }
        if (hasPendingClarification) {
            appendPendingClarificationBlock(sb, pendingClarification);
        }
        sb.append("</context>\n\n");
        sb.append('<').append(inputTag).append(">\n");
        sb.append(PromptXml.text(input));
        sb.append("\n</").append(inputTag).append(">\n");
        return sb.toString();
    }

    /** Renders the host-owned continuation state separately from the commander's new words. */
    private static void appendPendingClarificationBlock(StringBuilder sb, PendingClarification pending) {
        sb.append("<pending_clarification>\n");
        sb.append("  <action_id>").append(PromptXml.text(pending.actionId())).append("</action_id>\n");
        sb.append("  <missing_parameter>").append(PromptXml.text(pending.parameterName()))
                .append("</missing_parameter>\n");
        sb.append("  <original_command>").append(PromptXml.text(pending.originalInput()))
                .append("</original_command>\n");
        sb.append("  <question_asked>").append(PromptXml.text(pending.question()))
                .append("</question_asked>\n");
        sb.append("</pending_clarification>\n");
    }

    private String buildNarrationInput(String currentInput, List<AmbientContextNote> ambient) {
        String input = currentInput == null ? "" : currentInput;
        boolean hasAmbient = ambient != null && !ambient.isEmpty();
        if (!hasAmbient) {
            return input;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<context>\n");
        sb.append("The following facts and notes are trusted context, not words spoken by the commander.\n");
        appendAmbientBlock(sb, ambient);
        sb.append("</context>\n\n");
        // The narration input is already trusted prompt XML assembled by Thought.eventReaction().
        sb.append(input);
        return sb.toString();
    }

    private void appendAmbientBlock(StringBuilder sb, List<AmbientContextNote> ambient) {
        sb.append("<ambient_context>\n");
        int id = 1;
        for (AmbientContextNote note : ambient) {
            sb.append("  <note id=\"").append(id++).append("\" source=\"").append(note.source()).append("\">")
                    .append(PromptXml.text(note.text())).append("</note>\n");
        }
        sb.append("</ambient_context>\n");
    }

    /** One non-dialogue timeline entry to inline as current-turn context instead of a mid-dialogue system message. */
    private record AmbientContextNote(String source, String text) {}

    /** The replayed short-term history split into its two outputs: role-alternating messages and inlined ambient notes. */
    private record ReplayedHistory(List<LlmMessage> messages, List<AmbientContextNote> ambient) {}
}
