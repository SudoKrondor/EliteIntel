package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.gameapi.EventBusManager;
import elite.intel.session.Status;
import elite.intel.session.StatusFlags;
import elite.intel.session.ui.CommsPanel;
import elite.intel.session.ui.UINavigator;
import elite.intel.util.StringUtls;

public class OpenSquadronHandler implements CommandHandler {


    private final UINavigator navigator = new UINavigator();
    private final Status status = Status.getInstance();

    @Override
    public void handle(String action, JsonObject params, String responseText) {
        if (status.isInMainShip() || status.isInFighter()) {
            navigator.openAndNavigate(StatusFlags.GuiFocus.COMMS_PANEL, CommsPanel.SQUADRON);
        } else {
            EventBusManager.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("handler.common.cantDoNow")));
        }
    }
}
