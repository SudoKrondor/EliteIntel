package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

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

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_HARDPOINTS_TOGGLE;

/**
 * Stage-4b self-describing command for "deploy hardpoints".
 * Owns its own execution (ownsExecution() == true): the dispatch map routes this
 * command's execute() in place of the legacy DeployHardpointsHandler.
 */
@RegisterCommand
public final class DeployHardpointsCommand implements IntelCommand {

    @Override
    public String id() {
        return CommandIds.DEPLOY_HARDPOINTS;
    }

    @Override
    public boolean ownsExecution() {
        return true;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        Status status = Status.getInstance();

        if (status.isHardpointsDeployed()) {
            EventBusManager.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedLlm("handler.hardpoints.alreadyDeployed")));
        } else {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_HARDPOINTS_TOGGLE.getGameBinding())));
        }
    }
}
