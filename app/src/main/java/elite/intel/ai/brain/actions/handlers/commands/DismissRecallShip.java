package elite.intel.ai.brain.actions.handlers.commands;

import com.google.gson.JsonObject;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.GameControllerBus;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import static elite.intel.ai.hands.Bindings.GameCommand.*;

public class DismissRecallShip implements CommandHandler {

    private final Status status = Status.getInstance();

    @Override public void handle(String action, JsonObject params, String responseText) {
        if (status.isInSrv()) {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_RECALL_DISMISS_SHIP.getGameBinding())));
        } else if (status.isOnFoot()) {
            GameControllerBus.publish(GameInputSequenceEvent.of(
                    GameInputStep.bindingHold(BINDING_ON_FOOT_WHEEL.getGameBinding(), 500),
                    GameInputStep.bindingTap(BINDING_UI_LEFT.getGameBinding()),
                    GameInputStep.bindingTap(BINDING_UI_UP.getGameBinding()),
                    GameInputStep.bindingTap(BINDING_ACTIVATE.getGameBinding()),
                    GameInputStep.bindingTap(BINDING_EXIT_KEY.getGameBinding())
            ));
        } else if (status.isInMainShip()) {
            EventBusManager.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("speech.shipDismissRejected")));
            return;
        }
        if (status.isLanded()) {
            EventBusManager.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("speech.shipDismissed")));
        } else {
            EventBusManager.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("speech.shipRecall")));
        }
    }
}
