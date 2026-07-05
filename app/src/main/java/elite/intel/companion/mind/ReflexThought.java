package elite.intel.companion.mind;

import com.google.gson.JsonObject;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.llm.LlmToolInvocation;
import elite.intel.companion.model.memory.MemoryImportance;

/**
 * A reflex: a commander input the {@code ReflexResolver} matched verbatim to exactly one safe, parameterless
 * command. It runs on the commander lane like a {@link CommanderThought}, but skips the LLM entirely - there
 * is no prompt, no thinking loop and no tool selection. It just executes the resolved command; a command is a
 * side effect, not dialogue, so nothing is filed to memory (neither the imperative nor a call echo), and the
 * handler owns any spoken outcome.
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
        // A reflex is always a COMMAND - a side effect, not dialogue - so neither the commander's imperative nor
        // the call echo is filed to memory (they carry nothing worth recalling and would only clutter the
        // short-term timeline and the prompt). Executed with no tool-call id: any handler-voiced outcome is then
        // remembered as a plain companion line rather than a linked tool result, so nothing is left orphaned.
        LlmToolInvocation inv = new LlmToolInvocation(newId(), commandId, new JsonObject());
        execute(inv, null);
    }

    /** The live global conversation topic, exactly as a commander thought tags its memory. */
    @Override
    protected ConversationTopic memoryTopic() {
        return ctx.state().globalTopic();
    }

    /** A reflex runs no LLM, so it cannot rate the turn: its memory is ordinary importance. */
    @Override
    protected MemoryImportance memoryImportance() {
        return MemoryImportance.NORMAL;
    }
}
