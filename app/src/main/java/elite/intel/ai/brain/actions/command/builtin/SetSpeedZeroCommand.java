package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class SetSpeedZeroCommand extends SimpleTapCommand {
    public static final String ID = "set_speed_to_zero_0_stop_ship";

    public SetSpeedZeroCommand() {
        super(ID, Bindings.GameCommand.BINDING_SET_SPEED_ZERO.getGameBinding());
    }
}
