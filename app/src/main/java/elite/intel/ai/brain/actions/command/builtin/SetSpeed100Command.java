package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class SetSpeed100Command extends SimpleTapCommand {
    public SetSpeed100Command() {
        super(CommandIds.SET_SPEED_100, Bindings.GameCommand.BINDING_SET_SPEED100.getGameBinding());
    }
}
