package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.eventbus.GameEventBus;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_LANDING_GEAR_TOGGLE;

/**
 * Stage-4b self-describing command for "deploy landing gear".
 */
@RegisterCommand
public final class DeployLandingGearCommand implements IntelCommand {
    public static final String ID = "deploy_landing_gear";

    @Override public String llmDescription() { return "Deploy the landing gear."; }


    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();

        if (status.isLandingGearDown()) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.landingGear.alreadyDeployed")));
        } else {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_LANDING_GEAR_TOGGLE.getGameBinding())));
        }
    }
}
