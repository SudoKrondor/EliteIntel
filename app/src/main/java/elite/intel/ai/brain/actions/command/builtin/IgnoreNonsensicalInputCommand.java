package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;

/**
 * Stage-4b self-describing command for "ignore nonsensical input".
 * Intentional
 * no-op, matching the legacy handler 1:1.
 */
@RegisterCommand
public final class IgnoreNonsensicalInputCommand implements IntelCommand {
    public static final String ID = "ignore_nonsensical_input";


    @Override
    public String id() {
        return ID;
    }

    @Override
    public JsonObject execute(JsonObject params, String responseText) {
        // do nothing
        return null;
    }
}
