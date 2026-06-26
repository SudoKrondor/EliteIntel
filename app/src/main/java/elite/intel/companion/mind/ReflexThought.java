package elite.intel.companion.mind;

import com.google.gson.JsonObject;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.llm.LlmToolInvocation;

import java.util.List;

/**
 * A reflex: a commander input the {@code ReflexResolver} matched verbatim to exactly one safe, parameterless
 * command. It runs on the commander lane like a {@link CommanderThought}, but skips the LLM entirely - there
 * is no prompt, no thinking loop and no tool selection. It records the input, executes the resolved command,
 * and voices/remembers the outcome through the shared {@link #recordOutcome} (the command's own COMMAND path:
 * the handler's text or an affirmative ack, plus a compact timeline entry).
 * <p>
 * No interrupt handling (§1.9.41): the resolver only admits a fast, parameterless command, and a started
 * command is never cancelled - so a reflex simply runs to completion.
 */
final class ReflexThought extends Thought {

    private final String commandId;

    ReflexThought(Urgency urgency, String input, String commandId, ThoughtContext ctx) {
        super(ThoughtSource.COMMANDER, urgency, input, ctx);
        this.commandId = commandId;
    }

    @Override
    public void run() {
        recordCurrentInput(); // file the commander's words ([COMMANDER]) before the command runs (§2.6)
        LlmToolInvocation inv = new LlmToolInvocation(newId(), commandId, new JsonObject());
        JsonObject result = execute(inv);
        recordOutcome(inv, result, List.of()); // COMMAND outcome: handler text / ack + compact memory
    }

    /** The live global conversation topic, exactly as a commander thought tags its memory. */
    @Override
    protected ConversationTopic memoryTopic() {
        return ctx.state().globalTopic();
    }
}
