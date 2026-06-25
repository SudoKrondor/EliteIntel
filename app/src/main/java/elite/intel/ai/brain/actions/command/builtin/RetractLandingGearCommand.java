package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.CommandOutcome;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_LANDING_GEAR_TOGGLE;

/**
 * Stage-4b self-describing command for "retract landing gear".
 */
@RegisterCommand
public final class RetractLandingGearCommand implements IntelCommand {
    public static final String ID = "retract_landing_gear";

    @Override public String llmDescription() { return "Retract the landing gear."; }


    @Override
    public String id() {
        return ID;
    }

    @Override
    public JsonObject execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();

        if (status.isDocked() || status.isLanded() || status.isOnFoot() || status.isInFighter()) {
            return CommandOutcome.critical(StringUtls.localizedLlm("handler.landingGear.cantDoThat"));
        }

        if (status.isInMainShip() && status.isLandingGearDown()) {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_LANDING_GEAR_TOGGLE.getGameBinding())));
            return null;
        }
        return CommandOutcome.critical(StringUtls.localizedLlm("handler.landingGear.alreadyRetracted"));
    }
}
