package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class DeployShieldCellCommand extends SimpleTapCommand {
    public static final String ID = "deploy_shield_cell";

    public DeployShieldCellCommand() {
        super(ID, Bindings.GameCommand.BINDING_USE_SHIELD_CELL.getGameBinding());
    }
}
