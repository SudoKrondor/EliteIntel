package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.GameControllerBus;
import elite.intel.session.Status;
import elite.intel.session.StatusFlags;
import elite.intel.session.ui.CommsPanel;
import elite.intel.session.ui.UINavigator;
import elite.intel.util.StringUtls;

import static elite.intel.ai.hands.Bindings.GameCommand.*;

public class OpenInboxPanelHandler implements CommandHandler {


    private final UINavigator navigator = new UINavigator();
    private final Status status = Status.getInstance();


    @Override
    public void handle(String action, JsonObject params, String responseText) {
        if (status.isInMainShip() || status.isInFighter() || status.isInSrv()) {
            navigator.openAndNavigate(StatusFlags.GuiFocus.COMMS_PANEL, CommsPanel.INBOX);
        } else if (status.isOnFoot()) {
            GameControllerBus.publish(GameInputSequenceEvent.of(
                    GameInputStep.bindingHold(BINDING_ON_FOOT_WHEEL.getGameBinding(), 500),
                    GameInputStep.delay(500),
                    GameInputStep.bindingTap(BINDING_UI_UP.getGameBinding()),
                    GameInputStep.bindingTap(BINDING_ACTIVATE.getGameBinding()),
                    GameInputStep.bindingTap(BINDING_CYCLE_NEXT_PANEL.getGameBinding())
            ));
        } else {
            EventBusManager.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("handler.common.cantDoNow")));
        }
    }
}
