package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class TargetWingman1Command extends SimpleTapCommand {
    public static final String ID = "target_wingman_1";

    public TargetWingman1Command() {
        super(ID, Bindings.GameCommand.BINDING_TARGET_WINGMAN0.getGameBinding());
    }
}
