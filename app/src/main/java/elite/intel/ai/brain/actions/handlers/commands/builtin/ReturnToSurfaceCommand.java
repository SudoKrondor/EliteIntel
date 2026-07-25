package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import static elite.intel.ai.hands.Bindings.GameCommand.*;

/**
 * Stage-4b self-describing command for "return ship to surface".
 * The legacy handler is
 * shared with "dismiss ship to orbit" and does not branch on action, so both commands
 * carry an identical body 1:1.
 */
@RegisterCommand
public final class ReturnToSurfaceCommand implements IntelCommand {
    public static final String ID = "return_to_surface";

    @Override
    public String llmDescription() {
        return "Recall the main ship to the commander's current location for pickup while the commander is in an SRV or on foot. The ship comes to the commander; this does not return the SRV or buggy to the ship.";
    }


    private final Status status = Status.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInSrv() || status.isOnFoot();
    }

    @Override
    public String execute(JsonObject params, String responseText) {
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
            return StringUtls.localizedResponse("speech.shipDismissRejected");
        }
        if (status.isLanded()) {
            return StringUtls.localizedResponse("speech.shipDismissed");
        } else {
            return StringUtls.localizedResponse("speech.shipRecall");
        }
    }
}
