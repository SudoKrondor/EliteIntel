package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class DeployHeatSinkCommand extends SimpleTapCommand {
    public static final String ID = "deploy_heat_sink";

    @Override public String llmDescription() { return "Deploy a heat sink."; }

    public DeployHeatSinkCommand() {
        super(ID, Bindings.GameCommand.BINDING_DEPLOY_HEAT_SINK.getGameBinding());
    }

    /** Heat-sink launcher: while flying the main ship (incl. supercruise, e.g. after fuel scooping); not docked/landed. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip() && !status.isDocked() && !status.isLanded();
    }
}
