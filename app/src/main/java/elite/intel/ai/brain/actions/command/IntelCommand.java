package elite.intel.ai.brain.actions.command;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.IntelAction;

/**
 * Self-describing built-in command. Owns its own metadata (id, parameter schema,
 * binding, isDangerous, voiceStrategy, description key) and its execution.
 * Provides a default {@link #handle} that delegates to {@link #execute}, so a command
 * drops straight into the handler map dispatched through {@link IntelAction}.
 */
public interface IntelCommand extends IntelAction {

    default boolean isDangerous() {
        return false;
    }

    default VoiceStrategy voiceStrategy() {
        return VoiceStrategy.CANNED;
    }

    /** i18n key for the lazy description; text resolution stays in CommandCatalog (Stage 3). */
    default String descriptionKey() {
        return CommandI18nKeys.descriptionKey(id());
    }

    /** ED game-binding name this command taps, or null when not a single-binding tap. */
    default String bindingName() {
        return null;
    }

    /** Catalog display kind. BINDING for single game-binding taps, ACTION otherwise.
     *  Self-described by the command; NOT derived from a legacy handler. */
    default CommandKind kind() {
        return CommandKind.ACTION;
    }

    /**
     * Executes the command and returns its outcome (see {@link elite.intel.ai.brain.actions.CommandOutcome}),
     * or {@code null} for a silent side-effect. The active conversational owner of the current mode renders
     * the outcome: the legacy {@code ResponseRouter} speaks it, the companion hands it back as a tool result.
     * Commands no longer narrate themselves by publishing voice events.
     */
    JsonObject execute(JsonObject params, String responseText);

    @Override
    default JsonObject handle(String action, JsonObject params, String responseText) {
        return execute(params, responseText);
    }
}
