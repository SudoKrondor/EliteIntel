package elite.intel.companion.mind;

import com.google.gson.JsonObject;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.llm.LlmToolInvocation;
import elite.intel.companion.model.memory.MemoryImportance;
import elite.intel.companion.tools.IntelActionTypeResolver.IntelActionType;

import java.util.List;

/**
 * A reflex: a commander input a reflex gate resolved to exactly one safe, parameterless action without the LLM -
 * either {@code ReflexResolver} (verbatim exact-alias, commands only) or {@code SemanticReflexResolver} (a
 * confident, unambiguous embedding match, commands or queries). It runs on the commander lane like a
 * {@link CommanderThought} but skips the LLM entirely - no prompt, no thinking loop, no tool selection.
 * <p>
 * A COMMAND reflex just executes the command: a side effect, not dialogue, so nothing is filed to memory and the
 * handler owns any spoken outcome. A QUERY reflex runs the query's own data-grounded analysis path and publishes
 * its answer (via {@link #recordOutcome}) - so the reply is delivered from data, never a model's whim - and
 * records the input/call so the answer pairs with its turn on replay.
 * <p>
 * No interrupt handling (§1.9.41): the resolver only admits a fast, parameterless action, and a started action is
 * never cancelled - so a reflex simply runs to completion.
 */
final class ReflexThought extends Thought {

    private final String actionId;

    ReflexThought(Urgency urgency, String input, String actionId, ThoughtContext ctx) {
        super(ThoughtSource.COMMANDER, urgency, input, ctx);
        this.actionId = actionId;
    }

    @Override
    public void run() {
        LlmToolInvocation inv = new LlmToolInvocation(newId(), actionId, new JsonObject());
        if (ctx.actionTypeResolver().resolve(actionId) == IntelActionType.QUERY) {
            // A query reflex: file the input (a query turn keeps its user turn for pair replay), run the query's
            // own data-grounded analysis path under a tool-call id, and publish the answer - the reply comes from
            // the data, not the model.
            recordCurrentInput();
            String toolCallId = newId();
            recordCall(toolCallId, inv);
            JsonObject result = execute(inv);
            recordOutcome(inv, result, List.of(), toolCallId);
            return;
        }
        // A command reflex - a side effect, not dialogue - so neither the imperative nor the call echo is filed
        // to memory. Any handler-voiced outcome is remembered as a plain companion line rather than a linked
        // tool result, so nothing is left orphaned.
        execute(inv);
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
