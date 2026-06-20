package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class SetSpeed50Command extends SimpleTapCommand {
    public static final String ID = "set_speed_50";

    public SetSpeed50Command() {
        super(ID, Bindings.GameCommand.BINDING_SET_SPEED50.getGameBinding());
    }
}
