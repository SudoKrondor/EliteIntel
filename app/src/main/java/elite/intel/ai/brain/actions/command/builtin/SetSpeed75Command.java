package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class SetSpeed75Command extends SimpleTapCommand {
    public SetSpeed75Command() {
        super(CommandIds.SET_SPEED_75, Bindings.GameCommand.BINDING_SET_SPEED75.getGameBinding());
    }
}
