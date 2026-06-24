package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.eventbus.GameEventBus;
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

    @Override public String llmDescription() { return "Return to the planet surface."; }


    private final Status status = Status.getInstance();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
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
            GameEventBus.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("speech.shipDismissRejected")));
            return;
        }
        if (status.isLanded()) {
            GameEventBus.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("speech.shipDismissed")));
        } else {
            GameEventBus.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("speech.shipRecall")));
        }
    }
}
