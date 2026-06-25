package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.gameapi.inputs.UiNavCommon;

/**
 * Self-describing "exit close panel" command.
 * Owns its own execution: body migrated 1:1 from the legacy ClosePanelHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class ExitCloseCommand implements IntelCommand {
    public static final String ID = "exit_close";

    @Override public String llmDescription() { return "Back out of or close the current menu or panel."; }


    @Override
    public String id() {
        return ID;
    }

    @Override
    public JsonObject execute(JsonObject params, String responseText) {
        UiNavCommon.close();
        return null;
    }
}
