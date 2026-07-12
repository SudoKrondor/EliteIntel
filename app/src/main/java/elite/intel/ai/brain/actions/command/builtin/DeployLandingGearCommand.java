package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
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

    @Override
    public String llmDescription() {
        return "Lower/extend the landing gear in preparation for docking or landing.";
    }


    @Override
    public String id() {
        return ID;
    }

    /// due to bug in FDev impl of the Status.json we can't rely on the status
    /// ALWAYS RETURN TRUE HERE
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();

        if (status.isLandingGearDown()) {
            return StringUtls.localizedLlm("handler.landingGear.alreadyDeployed");
        } else {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_LANDING_GEAR_TOGGLE.getGameBinding())));
        }
        return null;
    }
}
