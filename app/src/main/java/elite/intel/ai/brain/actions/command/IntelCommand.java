package elite.intel.ai.brain.actions.command;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.IntelAction;

/**
 * Self-describing built-in command. Owns its own metadata (id, parameter schema,
 * binding, isDangerous, voiceStrategy, description key) and its execution.
 * Extends CommandHandler so a discovered command can later (Stage 2) be dropped
 * straight into the existing handler map; default handle() delegates to execute().
 */
public interface IntelCommand extends CommandHandler, IntelAction {

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

    void execute(JsonObject params, String responseText);

    @Override
    default JsonObject handle(String action, JsonObject params, String responseText) {
        execute(params, responseText);
        return null;
    }
}
