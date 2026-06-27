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
     * Short English, provider-facing purpose of this action for tool-calling: what it does and what it
     * returns or affects. Authored in English on purpose (single cache prefix, English companion prompt);
     * distinct from the localized, UI-facing description behind {@code descriptionKey()}. Empty by default:
     * an unauthored action then falls back to its example phrases in the rendered tool description.
     */
    default String llmDescription() {
        return "";
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
