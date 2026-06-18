package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class SetSpeedZeroCommand extends SimpleTapCommand {
    public SetSpeedZeroCommand() {
        super(CommandIds.SET_SPEED_TO_ZERO_0_STOP_SHIP, Bindings.GameCommand.BINDING_SET_SPEED_ZERO.getGameBinding());
    }
}
