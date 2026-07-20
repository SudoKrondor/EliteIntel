package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.AIConstants;
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

    /** Runs the command and returns its spoken outcome (the line to voice and remember), or null when it has none. */
    String execute(JsonObject params, String responseText);

    @Override
    default JsonObject handle(String action, JsonObject params, String responseText) {
        // The command returns its spoken outcome, or null when it voiced directly (filler/narrator) or has nothing
        // to say - a null result is a silent side-effect dispatch, not an error.
        String outcome = execute(params, responseText);
        if (outcome == null || outcome.isBlank()) {
            return null;
        }
        JsonObject result = new JsonObject();
        result.addProperty(AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE, outcome);
        return result;
    }
}
