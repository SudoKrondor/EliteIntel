package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class DeployChaffCommand extends SimpleTapCommand {
    public static final String ID = "deploy_chaff";

    @Override public String llmDescription() { return "Deploy chaff."; }

    public DeployChaffCommand() {
        super(ID, Bindings.GameCommand.BINDING_FIRE_CHAFF_LAUNCHER.getGameBinding());
    }
}
