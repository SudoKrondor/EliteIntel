package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class SetSpeed50Command extends SimpleTapCommand {
    public SetSpeed50Command() {
        super(CommandIds.SET_SPEED_50, Bindings.GameCommand.BINDING_SET_SPEED50.getGameBinding());
    }
}
