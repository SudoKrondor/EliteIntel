package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class TargetWingman3Command extends SimpleTapCommand {
    public TargetWingman3Command() {
        super(CommandIds.TARGET_WINGMAN_3, Bindings.GameCommand.BINDING_TARGET_WINGMAN2.getGameBinding());
    }
}
