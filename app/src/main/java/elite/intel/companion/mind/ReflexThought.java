package elite.intel.companion.mind;

import com.google.gson.JsonObject;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.llm.LlmToolInvocation;
import elite.intel.companion.model.memory.MemoryImportance;
import elite.intel.companion.tools.IntelActionTypeResolver.IntelActionType;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A reflex: a commander input a reflex gate resolved to exactly one safe, parameterless action without the LLM -
 * either {@code ReflexResolver} (verbatim exact-alias, commands only) or {@code SemanticReflexResolver} (a
 * confident, unambiguous embedding match, commands or queries). It runs on the commander lane like a
 * {@link CommanderThought} but skips the LLM entirely - no prompt, no thinking loop, no tool selection.
 * <p>
 * A COMMAND reflex just executes the command: a side effect, not dialogue, so nothing is filed to memory and the
 * handler owns any spoken outcome. A QUERY reflex runs the query's own data-grounded analysis path and, only once
 * a non-blank answer exists, publishes input/CALL/RESULT together - so the reply is delivered from data, never a
 * model's whim, and an interrupted query leaves no partial turn on replay.
 * <p>
 * The handler detaches exactly like an LLM-selected game call. A queued future is cancellable; a handler already
 * started may finish operationally, but interruption discards its late speech and memory result.
 */
final class ReflexThought extends Thought {

    private final String actionId;
    private volatile ConversationTopic turnTopic;

    ReflexThought(ThoughtContext context, String actionId, ThoughtDependencies dependencies) {
        super(context, dependencies);
        this.actionId = actionId;
    }

    @Override
    public void run() {
        startLifecycle().join();
    }

    /** Starts the reflex handler without retaining the ordered commander cognitive worker. */
    @Override
    CompletableFuture<Void> startLifecycle() {
        if (isStopped()) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            return beginReflex();
        } catch (Throwable failure) {
            return CompletableFuture.failedFuture(failure);
        }
    }

    private CompletableFuture<Void> beginReflex() {
        turnTopic = dependencies.state().globalTopic();
        LlmToolInvocation inv = new LlmToolInvocation(newId(), actionId, new JsonObject());
        if (dependencies.actionTypeResolver().resolve(actionId) == IntelActionType.QUERY) {
            // A query reflex runs its data-grounded path under a tool-call id. Nothing is filed until a non-blank
            // result exists, when input/CALL/RESULT are published together - the reply comes from data, not the model.
            String toolCallId = newId();
            CompletableFuture<JsonObject> execution = submitExecution(inv);
            inFlight = execution;
            if (isStopped()) {
                execution.cancel(true);
            }
            return execution.handle((result, failure) -> {
                try {
                    if (isStopped()) {
                        return null;
                    }
                    JsonObject settled = failure == null
                            ? (result == null ? new JsonObject() : result)
                            : executionError(inv.name(), failure);
                    publishCompletedQuery(inv, settled, toolCallId);
                    return null;
                } finally {
                    if (inFlight == execution) {
                        inFlight = null;
                    }
                }
            });
        }
        // A command reflex is a side effect, not dialogue. Neither its imperative, call echo nor handler outcome
        // enters conversational memory; a non-blank outcome is still voiced by recordOutcome.
        CompletableFuture<JsonObject> execution = submitExecution(inv);
        inFlight = execution;
        if (isStopped()) {
            execution.cancel(true);
        }
        return execution.handle((result, failure) -> {
            try {
                if (isStopped()) {
                    return null;
                }
                JsonObject settled = failure == null
                        ? (result == null ? new JsonObject() : result)
                        : executionError(inv.name(), failure);
                recordOutcome(inv, settled, List.of(), null);
                return null;
            } finally {
                if (inFlight == execution) {
                    inFlight = null;
                }
            }
        });
    }

    /** The live global conversation topic, exactly as a commander thought tags its memory. */
    @Override
    protected ConversationTopic memoryTopic() {
        ConversationTopic frozen = turnTopic;
        return frozen != null ? frozen : dependencies.state().globalTopic();
    }

    /**
     * A reflex runs no LLM, so it cannot rate the turn - and an unrated turn is never a durable fact: its input
     * (a fast imperative or question) and any query answer are stamped LOW, kept only for hot-timeline continuity.
     * This keeps a reflexed imperative out of the answer-fact candidates - a NORMAL {@code COMMANDER} line would
     * otherwise read as a stated fact (see {@code MemoryFactCandidates.isTier2}) - since only the consciousness's
     * own LLM path (via {@code classify_turn}) promotes a turn to durable importance.
     */
    @Override
    protected MemoryImportance memoryImportance() {
        return MemoryImportance.LOW;
    }
}
