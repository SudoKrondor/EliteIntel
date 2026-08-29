package elite.intel.ai.brain.vega.mind;

import elite.intel.ai.brain.vega.model.IntelActionCategory;
import elite.intel.ai.brain.vega.model.Urgency;
import elite.intel.ai.brain.vega.model.llm.LlmResult;
import elite.intel.ai.brain.vega.model.llm.LlmToolDefinition;
import elite.intel.ai.brain.vega.model.llm.LlmToolInvocation;
import elite.intel.ai.brain.vega.prompt.ComposedPrompt;
import elite.intel.ai.brain.vega.tools.SpeakFunction;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Voices one gameplay reaction. Narration mode converts bounded transient event data into a line; verbatim mode
 * uses an already finished line. Neither mode stores anything: session memory is the conversation window the next
 * commander prompt replays, and a gameplay narration was never replayed into it.
 */
public final class EventThought extends Thought {

    /** Non-null selects verbatim mode; null selects one-round narration. */
    private final String verbatimText;

    /** Creates the narration mode from the event turn signals. */
    EventThought(ThoughtContext context, ThoughtDependencies dependencies) {
        this(context, null, dependencies);
    }

    /** Creates either mode. A non-null {@code verbatimText} selects verbatim mode. */
    EventThought(ThoughtContext context, String verbatimText, ThoughtDependencies dependencies) {
        super(context, dependencies);
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
     * Voices the finished line without an LLM round.
     */
    private void runVerbatim() {
        voice(verbatimText, urgency() == Urgency.URGENT);
    }

    /** Voices one valid LLM narration; a failed or interrupted round voices nothing. */
    private void runNarration() {
        ComposedPrompt prompt = composeInitialPrompt();
        LlmResult result = submitRound(prompt.messages(), prompt.tools(), prompt.profile());
        if (result == null || !result.isValid()) {
            return;
        }
        LlmToolInvocation invocation = result.toolInvocations().get(0);
        if (SpeakFunction.ID.equals(invocation.name())) {
            execute(invocation);
        }
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
    protected List<LlmToolDefinition> systemTools(List<LlmToolDefinition> gameTools) {
        return dependencies.systemFunctionProvider().systemFunctions(source(), gameTools);
    }
}
