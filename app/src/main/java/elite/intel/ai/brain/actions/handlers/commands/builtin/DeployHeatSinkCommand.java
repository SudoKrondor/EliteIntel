package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.actions.handlers.commands.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class DeployHeatSinkCommand extends SimpleTapCommand {
    public static final String ID = "deploy_heat_sink";

    @Override
    public String llmDescription() {
        return "Launch a heat sink to dump heat (cool the ship or drop off heat-seeking sensors).";
    }

    public DeployHeatSinkCommand() {
        super(ID, Bindings.GameCommand.BINDING_DEPLOY_HEAT_SINK.getGameBinding());
    }

    /** Heat-sink launcher: while flying the main ship (incl. supercruise, e.g. after fuel scooping); not docked/landed. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip() && !status.isDocked() && !status.isLanded();
    }
}
