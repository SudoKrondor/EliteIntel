package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.session.Status;
import elite.intel.session.StatusFlags;
import elite.intel.session.ui.RightPanel;
import elite.intel.session.ui.UINavigator;

/**
 * Stage-4b self-describing command for "show storage panel".
 * Guard intentionally
 * omits isInFighter() to match the legacy handler 1:1.
 */
@RegisterCommand
public final class ShowStoragePanelCommand implements IntelCommand {
    public static final String ID = "show_storage_panel";

    @Override
    public String llmDescription() {
        return "Open the ship storage panel (stored modules and ships at this station).";
    }


    private final UINavigator navigator = new UINavigator();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip() || status.isInSrv();
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        navigator.openAndNavigate(StatusFlags.GuiFocus.INTERNAL_PANEL, RightPanel.STORAGE);
    }
}
