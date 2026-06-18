package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.session.Status;
import elite.intel.session.StatusFlags;
import elite.intel.session.ui.CenterPanel;
import elite.intel.session.ui.UINavigator;

/**
 * Stage-4b self-describing command for "show fighter panel".
 * Owns its own execution (ownsExecution() == true): the dispatch map routes this
 * command's execute() in place of the legacy OpenFighterPanelHandler.
 */
@RegisterCommand
public final class ShowFighterPanelCommand implements IntelCommand {

    private final UINavigator navigator = new UINavigator();
    private final Status status = Status.getInstance();

    @Override
    public String id() {
        return CommandIds.SHOW_FIGHTER_PANEL;
    }

    @Override
    public boolean ownsExecution() {
        return true;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        if (status.isInMainShip() || status.isInSrv() || status.isInFighter()) {
            navigator.openAndNavigate(StatusFlags.GuiFocus.ROLE_PANEL, CenterPanel.FIGHTER);
        }
    }
}
