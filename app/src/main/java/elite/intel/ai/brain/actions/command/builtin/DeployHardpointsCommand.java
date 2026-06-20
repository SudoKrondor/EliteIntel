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

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_HARDPOINTS_TOGGLE;

/**
 * Stage-4b self-describing command for "deploy hardpoints".
 */
@RegisterCommand
public final class DeployHardpointsCommand implements IntelCommand {
    public static final String ID = "deploy_hardpoints";


    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();

        if (status.isHardpointsDeployed()) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.hardpoints.alreadyDeployed")));
        } else {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_HARDPOINTS_TOGGLE.getGameBinding())));
        }
    }
}
