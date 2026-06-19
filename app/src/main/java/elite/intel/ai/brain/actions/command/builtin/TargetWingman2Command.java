package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class TargetWingman2Command extends SimpleTapCommand {
    public TargetWingman2Command() {
        super(CommandIds.TARGET_WINGMAN_2, Bindings.GameCommand.BINDING_TARGET_WINGMAN1.getGameBinding());
    }
}
