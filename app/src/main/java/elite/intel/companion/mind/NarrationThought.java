package elite.intel.companion.mind;

import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.llm.LlmResult;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.companion.model.llm.LlmToolInvocation;
import elite.intel.companion.prompt.ComposedPrompt;
import elite.intel.companion.tools.SpeakFunction;

import java.util.List;

/**
 * A thought born from subscriber-prepared sensor narration. The curated subscriber layer already decided
 * this is worth saying and pre-digested the data, so the LLM's only job is to phrase the provided content in
 * character. Its turn is deliberately tiny: one LLM round, voice the {@code speak} line, remember it - done.
 * <p>
 * Unlike the commander loop it does not loop, has no game tools, runs no actions, and records no raw input:
 * the only memory it leaves is the {@code COMPANION} line it actually said (the raw sensor data is not
 * persisted). Its memory tag is the topic supplied by the subscriber layer; like any non-commander thought
 * it never moves the global conversation topic.
 */
public final class NarrationThought extends Thought {

    private final ConversationTopic eventTopic;

    NarrationThought(Urgency urgency, String summary, ConversationTopic eventTopic, ThoughtContext ctx) {
        super(ThoughtSource.NARRATION, urgency, summary, ctx);
        this.eventTopic = eventTopic;
    }

    /**
     * Single short round: compose the lean narration prompt, ask the LLM to phrase it once, then voice the
     * {@code speak} line and record it as the companion's own words. Best-effort - a failed or interrupted
     * round simply stays silent. Nothing else happens this turn (no second round, no raw-data record).
     */
    @Override
    public void run() {
        ComposedPrompt prompt = composeInitialPrompt();
        LlmResult result = submitRound(prompt.messages(), prompt.tools(), prompt.profile());
        if (result == null || !result.isValid()) {
            return;
        }
        for (LlmToolInvocation inv : result.toolInvocations()) {
            if (SpeakFunction.ID.equals(inv.name())) {
                execute(inv);                              // voice the phrased line through the speech gateway
                recordCompanionSpeech(spokenTextOf(inv));  // remember what we said, not the raw sensor data
            }
        }
    }

    @Override
    protected ConversationTopic memoryTopic() {
        return eventTopic;
    }

    // Game-tool categories come from IntelActionAccessPolicy for NARRATION (none); inherited from the base,
    // so the prompt offers no commands and no queries.

    /**
     * System tools for the NARRATION source ({@code speak} + {@code nothing_to_do}), with {@code speak}
     * ungated: the subscriber layer already decided this should be narrated, so verbosity does not suppress it.
     */
    @Override
    protected List<LlmToolDefinition> systemTools() {
        return ctx.systemFunctionProvider().systemFunctions(source());
    }
}
