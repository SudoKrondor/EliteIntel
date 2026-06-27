package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class TargetWingman3Command extends SimpleTapCommand {
    public static final String ID = "target_wingman_3";

    @Override public String llmDescription() { return "Target wingman 3."; }

    public TargetWingman3Command() {
        super(ID, Bindings.GameCommand.BINDING_TARGET_WINGMAN2.getGameBinding());
    }
}
