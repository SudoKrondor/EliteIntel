package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class FighterHoldFireCommand extends SimpleTapCommand {
    public FighterHoldFireCommand() {
        super(CommandIds.FIGHTER_HOLD_FIRE, Bindings.GameCommand.BINDING_REQUEST_HOLD_FIRE.getGameBinding());
    }
}
