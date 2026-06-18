package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class SetSpeed25Command extends SimpleTapCommand {
    public SetSpeed25Command() {
        super(CommandIds.SET_SPEED_25, Bindings.GameCommand.BINDING_SET_SPEED25.getGameBinding());
    }
}
