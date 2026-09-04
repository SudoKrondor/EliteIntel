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
 * Stage-4b self-describing command for "dismiss ship to orbit".
 * <p>
 * The keystrokes are the same as {@link ReturnToSurfaceCommand}'s - Frontier binds recall and dismiss to one
 * toggle - but the spoken answer is not: each command says what was asked of it.
 */
@RegisterCommand
public final class DismissShipToOrbitCommand implements IntelCommand {
    public static final String ID = "dismiss_ship_to_orbit";

    @Override
    public String llmDescription() {
        return "Send the ship away to orbit while in the SRV or on foot (dismiss the recalled ship); not for use while piloting the ship.";
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
        // The answer is the commander's own order, not a guess at which way the toggle went. Reading it off
        // the landed flag answered "coming back to get you" to every dismissal made while the ship was not
        // sitting beside them - which is most of them.
        return StringUtls.localizedResponse("speech.shipDismissed");
    }
}
