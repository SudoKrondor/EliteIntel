package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class DeployShieldCellCommand extends SimpleTapCommand {
    public static final String ID = "deploy_shield_cell";

    @Override
    public String llmDescription() {
        return "Activate a shield cell bank (SCB) to recharge the ship's shields.";
    }

    public DeployShieldCellCommand() {
        super(ID, Bindings.GameCommand.BINDING_USE_SHIELD_CELL.getGameBinding());
    }

    /** Defensive module: while flying the main ship (shields up incl. supercruise); not docked/landed. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip() && !status.isDocked() && !status.isLanded();
    }
}
