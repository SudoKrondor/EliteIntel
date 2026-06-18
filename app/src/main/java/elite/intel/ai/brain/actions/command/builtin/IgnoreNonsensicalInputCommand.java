package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;

/**
 * Stage-4b self-describing command for "ignore nonsensical input".
 * Owns its own execution (ownsExecution() == true): the dispatch map routes this
 * command's execute() in place of the legacy IgnoreNonSensicalInputHandler. Intentional
 * no-op, matching the legacy handler 1:1.
 */
@RegisterCommand
public final class IgnoreNonsensicalInputCommand implements IntelCommand {

    @Override
    public String id() {
        return CommandIds.IGNORE_NONSENSICAL_INPUT;
    }

    @Override
    public boolean ownsExecution() {
        return true;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        // do nothing
    }
}
