package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.gameapi.EventBusManager;
import elite.intel.session.Status;
import elite.intel.session.StatusFlags;
import elite.intel.session.ui.CommsPanel;
import elite.intel.session.ui.UINavigator;
import elite.intel.util.StringUtls;

/**
 * Stage-4b self-describing command for "show squadron panel".
 * Owns its own execution (ownsExecution() == true): the dispatch map routes this
 * command's execute() in place of the legacy OpenSquadronHandler. No on-foot path; else
 * branch announces cantDoNow, matching the legacy handler 1:1.
 */
@RegisterCommand
public final class ShowSquadronPanelCommand implements IntelCommand {

    private final UINavigator navigator = new UINavigator();
    private final Status status = Status.getInstance();

    @Override
    public String id() {
        return CommandIds.SHOW_SQUADRON_PANEL;
    }

    @Override
    public boolean ownsExecution() {
        return true;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        if (status.isInMainShip() || status.isInFighter()) {
            navigator.openAndNavigate(StatusFlags.GuiFocus.COMMS_PANEL, CommsPanel.SQUADRON);
        } else {
            EventBusManager.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("handler.common.cantDoNow")));
        }
    }
}
