package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.session.Status;

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_SET_SPEED75;

/**
 * Stage-4b self-describing command for "set optimal speed".
 */
@RegisterCommand
public final class SetOptimalSpeedCommand implements IntelCommand {
    public static final String ID = "set_optimal_speed";

    @Override
    public String llmDescription() {
        return "Set the throttle to 75%, the optimal approach speed (the supercruise 'blue zone' sweet spot for decelerating on approach).";
    }


    @Override
    public String id() {
        return ID;
    }

    /** Ship throttle: only while piloting the main ship and not docked/landed (no throttle when stationary). */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return (status.isInMainShip() || status.isInSrv() || status.isInFighter()) && (!status.isDocked() && !status.isLanded());
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_SET_SPEED75.getGameBinding()))); /// Sets to 75%
    }
}
