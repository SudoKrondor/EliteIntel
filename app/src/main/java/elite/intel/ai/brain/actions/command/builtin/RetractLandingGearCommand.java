package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.GameControllerBus;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_LANDING_GEAR_TOGGLE;

/**
 * Stage-4b self-describing command for "retract landing gear".
 */
@RegisterCommand
public final class RetractLandingGearCommand implements IntelCommand {
    public static final String ID = "retract_landing_gear";


    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();

        if (status.isDocked() || status.isLanded() || status.isOnFoot() || status.isInFighter()) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.landingGear.cantDoThat")));
            return;
        }

        if (status.isInMainShip() && status.isLandingGearDown()) {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_LANDING_GEAR_TOGGLE.getGameBinding())));
        } else {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.landingGear.alreadyRetracted")));
        }
    }
}
