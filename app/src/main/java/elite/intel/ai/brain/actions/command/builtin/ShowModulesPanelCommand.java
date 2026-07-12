package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.session.Status;
import elite.intel.session.StatusFlags;
import elite.intel.session.ui.RightPanel;
import elite.intel.session.ui.UINavigator;

/**
 * Stage-4b self-describing command for "show modules panel".
 */
@RegisterCommand
public final class ShowModulesPanelCommand implements IntelCommand {
    public static final String ID = "show_modules_panel";

    @Override
    public String llmDescription() {
        return "Open the modules panel (installed ship modules and their power priority).";
    }


    private final UINavigator navigator = new UINavigator();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip() || status.isInSrv() || status.isInFighter();
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        navigator.openAndNavigate(StatusFlags.GuiFocus.INTERNAL_PANEL, RightPanel.MODULES);
        return null;
    }
}
