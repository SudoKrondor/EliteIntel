package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class DeployHeatSinkCommand extends SimpleTapCommand {
    public DeployHeatSinkCommand() {
        super(CommandIds.DEPLOY_HEAT_SINK, Bindings.GameCommand.BINDING_DEPLOY_HEAT_SINK.getGameBinding());
    }
}
