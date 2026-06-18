package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class DeployShieldCellCommand extends SimpleTapCommand {
    public DeployShieldCellCommand() {
        super(CommandIds.DEPLOY_SHIELD_CELL, Bindings.GameCommand.BINDING_USE_SHIELD_CELL.getGameBinding());
    }
}
