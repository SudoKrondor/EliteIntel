package elite.intel.ai.brain.actions;

import com.google.gson.JsonObject;
import elite.intel.session.Status;
import java.util.List;

/**
 * Self-describing, invokable action shared by IntelCommand and IntelQuery.
 * Carries both the metadata the LLM action-map generator needs (stable id,
 * context visibility, parameter schema) and the runtime dispatch contract
 * {@link #handle}. Built-in commands/queries and runtime executors (e.g.
 * {@code CustomCommandHandler}) implement this single contract, so the
 * command/query handler maps are keyed on IntelAction.
 */
public interface IntelAction {
    String id();

    default boolean isVisibleForLLM(Status status) {
        return true;
    }

    default List<ActionParameterSpec> parameters() {
        return List.of();
    }

    /**
     * Executes this action. For commands the returned value is ignored
     * (side-effect only); for queries it carries the response payload as JSON.
     *
     * @param action the invoked action id
     * @param params invocation parameters extracted by the LLM
     * @param text   the command response text, or the original user input for queries
     * @return query response payload, or {@code null} for side-effect-only commands
     */
    JsonObject handle(String action, JsonObject params, String text) throws Exception;
}
