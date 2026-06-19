package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.session.Status;
import elite.intel.session.StatusFlags;
import elite.intel.session.ui.RightPanel;
import elite.intel.session.ui.UINavigator;

/**
 * Stage-4b self-describing command for "show fire groups panel".
 */
@RegisterCommand
public final class ShowFireGroupsPanelCommand implements IntelCommand {

    private final UINavigator navigator = new UINavigator();
    private final Status status = Status.getInstance();

    @Override
    public String id() {
        return CommandIds.SHOW_FIRE_GROUPS_PANEL;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        if (status.isInMainShip() || status.isInSrv() || status.isInFighter()) {
            navigator.openAndNavigate(StatusFlags.GuiFocus.INTERNAL_PANEL, RightPanel.FIRE_GROUPS);
        }
    }
}
