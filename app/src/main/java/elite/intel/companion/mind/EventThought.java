package elite.intel.companion.mind;

import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.IntelActionCategory;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.llm.LlmResult;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.companion.model.llm.LlmToolInvocation;
import elite.intel.companion.model.memory.MemoryImportance;
import elite.intel.companion.prompt.ComposedPrompt;
import elite.intel.companion.tools.SpeakFunction;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * A thought born from a gameplay subscriber that wants the companion to <b>voice the result</b> of reacting to a
 * game event. The subscriber already decided this is worth saying and pre-digested the data. Two modes, both a
 * tiny turn that leaves a clean two-party {@code user -> assistant} pair in memory (the stimulus as a
 * {@code user} turn, the spoken line as the companion's own words) so the timeline never grows an orphan
 * {@code assistant} line:
 * <ul>
 *   <li><b>narration</b> - the subscriber hands raw-but-digested data plus phrasing instructions; one LLM round
 *       (the lean Narration prompt) compresses it into a spoken line. The stimulus recorded is the digested data.</li>
 *   <li><b>verbatim</b> - the subscriber hands a finished phrase; it is voiced as-is with no LLM. The stimulus
 *       recorded is only a short source/event id (never the raw data, which could be a huge list that would
 *       bloat the prompt), and the finished phrase is recorded as if it were the LLM's reply.</li>
 * </ul>
 * It has no game tools and never moves the global conversation topic - its memory tag is the subscriber-supplied
 * topic.
 */
public final class EventThought extends Thought {

    private final ConversationTopic eventTopic;
    /**
     * Non-null selects verbatim mode: the finished phrase to voice as-is and record as the companion's reply,
     * with no LLM round. Null selects narration mode (one LLM round phrases the stimulus).
     */
    private final String verbatimText;

    /**
     * Narration constructor.
     *
     * @param stimulus    the digested event data, recorded verbatim as the {@code user} turn (the {@code currentInput})
     * @param promptInput the LLM-visible current input (tagged event data plus optional phrasing instructions)
     */
    EventThought(Urgency urgency, String stimulus, String promptInput, ConversationTopic eventTopic, ThoughtContext ctx) {
        this(urgency, stimulus, promptInput, null, eventTopic, ctx);
    }

    /**
     * Full constructor. A non-null {@code verbatimText} selects verbatim mode; in that mode {@code stimulus} is
     * the short source/event id (the {@code user} turn) and {@code promptInput} is unused.
     */
    EventThought(Urgency urgency, String stimulus, String promptInput, String verbatimText,
                 ConversationTopic eventTopic, ThoughtContext ctx) {
        super(ThoughtSource.EVENT, urgency, stimulus, promptInput, ctx);
        this.eventTopic = eventTopic;
        this.verbatimText = verbatimText;
    }

    @Override
    public void run() {
        if (verbatimText != null) {
            runVerbatim();
        } else {
            runNarration();
        }
    }

    /**
     * Verbatim result: record the short source id as the {@code user} turn, voice the finished phrase, and record
     * it as the companion's reply - a clean {@code user -> assistant} pair, no LLM. A blank phrase is not recorded.
     */
    private void runVerbatim() {
        recordCurrentInput();                              // user turn = the short source/event id
        voice(verbatimText, urgency() == Urgency.URGENT);  // voice the finished phrase (urgent preempts)
        recordCompanionSpeech(verbatimText);               // remember it as the companion's reply
    }

    /**
     * Narration result: one short round - compose the lean prompt, ask the LLM to phrase the stimulus once, then
     * record the stimulus as a {@code user} turn, voice the first {@code speak} line, and record it as the
     * companion's own words. Best-effort - a failed or interrupted round stays silent and records nothing (a
     * reaction with no reply is not a dialogue turn).
     */
    private void runNarration() {
        ComposedPrompt prompt = composeInitialPrompt();
        LlmResult result = submitRound(prompt.messages(), prompt.tools(), prompt.profile());
        if (result == null || !result.isValid()) {
            return;
        }
        // Only the first speak is voiced. A model that splits the line into more than one speak in a single
        // round (small models sometimes do) would otherwise be read twice; the extras are dropped.
        for (LlmToolInvocation inv : result.toolInvocations()) {
            if (SpeakFunction.ID.equals(inv.name())) {
                recordCurrentInput();                      // user turn = the digested stimulus
                execute(inv);                              // voice the phrased line through the speech gateway
                recordCompanionSpeech(spokenTextOf(inv));  // remember what we said, not the phrasing instructions
                return;
            }
        }
    }

    @Override
    protected ConversationTopic memoryTopic() {
        return eventTopic;
    }

    /** Event reactions carry ordinary importance; only the commander rates a turn (classify_turn). */
    @Override
    protected MemoryImportance memoryImportance() {
        return MemoryImportance.NORMAL;
    }

    /** No game tools: the subscriber already calculated and filtered the data, so the reducer offers nothing. */
    @Override
    protected Set<IntelActionCategory> allowedCategories() {
        return EnumSet.noneOf(IntelActionCategory.class);
    }

    /**
     * System tools for the EVENT source ({@code speak} only, narration mode); the subscriber decided this should
     * be narrated, so it is voiced unconditionally. Unused in verbatim mode (no prompt is composed).
     */
    @Override
    protected List<LlmToolDefinition> systemTools() {
        return ctx.systemFunctionProvider().systemFunctions(source());
    }
}
