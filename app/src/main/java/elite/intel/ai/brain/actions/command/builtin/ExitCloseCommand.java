package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.UiNavCommon;

/**
 * Self-describing "exit close panel" command.
 * Owns its own execution: body migrated 1:1 from the legacy ClosePanelHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class ExitCloseCommand implements IntelCommand {

    @Override
    public String id() {
        return CommandIds.EXIT_CLOSE;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        UiNavCommon.close();
    }
}
