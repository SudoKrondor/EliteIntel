package elite.intel.companion.mind;

import com.google.gson.JsonObject;
import elite.intel.companion.model.llm.LlmToolInvocation;

import java.util.concurrent.CompletableFuture;

/**
 * A reflex: {@code ReflexResolver} matched a commander input verbatim to exactly one safe, parameterless action.
 * It runs on the commander lane like a {@link CommanderThought} but skips the LLM entirely - no prompt, no thinking
 * loop, no tool selection. Every non-exact input goes through {@link CommanderThought}.
 * <p>
 * A COMMAND reflex just executes the command: a side effect, not dialogue, so nothing is filed to memory and the
 * handler owns any spoken outcome. A QUERY reflex runs the query's own data-grounded analysis path and publishes
 * a commander/companion pair only after a successful non-blank answer exists.
 * <p>
 * The handler detaches exactly like an LLM-selected game call. A queued future is cancellable; a handler already
 * started may finish operationally, but interruption discards its late speech and memory result.
 */
final class ReflexThought extends Thought {

    private final String actionId;

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
        LlmToolInvocation inv = new LlmToolInvocation(newId(), actionId, new JsonObject());
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
                settleToolOutcome(inv, settled);
                return null;
            } finally {
                if (inFlight == execution) {
                    inFlight = null;
                }
            }
        });
    }

}
