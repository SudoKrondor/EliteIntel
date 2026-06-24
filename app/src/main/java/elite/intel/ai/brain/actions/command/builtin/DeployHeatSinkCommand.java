package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class DeployHeatSinkCommand extends SimpleTapCommand {
    public static final String ID = "deploy_heat_sink";

    @Override public String llmDescription() { return "Deploy a heat sink."; }

    public DeployHeatSinkCommand() {
        super(ID, Bindings.GameCommand.BINDING_DEPLOY_HEAT_SINK.getGameBinding());
    }
}
