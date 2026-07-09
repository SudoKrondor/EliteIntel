package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.gameapi.inputs.UiNavCommon;
import elite.intel.session.Status;

/**
 * Self-describing "exit close panel" command.
 * Owns its own execution: body migrated 1:1 from the legacy ClosePanelHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class ExitCloseCommand implements IntelCommand {
    public static final String ID = "exit_close";

    @Override
    public String llmDescription() {
        return "Close or back out of the currently open panel, menu, or map (galaxy/system map).";
    }


    @Override
    public String id() {
        return ID;
    }

    /**
     * Available anywhere, including during hyperspace jump
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        UiNavCommon.close();
        return null;
    }
}
