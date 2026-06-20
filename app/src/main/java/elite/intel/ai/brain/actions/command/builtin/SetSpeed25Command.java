package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class SetSpeed25Command extends SimpleTapCommand {
    public static final String ID = "set_speed_25";

    public SetSpeed25Command() {
        super(ID, Bindings.GameCommand.BINDING_SET_SPEED25.getGameBinding());
    }
}
