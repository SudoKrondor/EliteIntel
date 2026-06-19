package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class DeployChaffCommand extends SimpleTapCommand {
    public DeployChaffCommand() {
        super(CommandIds.DEPLOY_CHAFF, Bindings.GameCommand.BINDING_FIRE_CHAFF_LAUNCHER.getGameBinding());
    }
}
