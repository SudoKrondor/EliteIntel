package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class SetSpeed100Command extends SimpleTapCommand {
    public static final String ID = "set_speed_100";

    public SetSpeed100Command() {
        super(ID, Bindings.GameCommand.BINDING_SET_SPEED100.getGameBinding());
    }
}
