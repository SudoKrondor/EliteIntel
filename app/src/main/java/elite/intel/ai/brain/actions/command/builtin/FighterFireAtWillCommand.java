package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class FighterFireAtWillCommand extends SimpleTapCommand {
    public FighterFireAtWillCommand() {
        super(CommandIds.FIGHTER_FIRE_AT_WILL, Bindings.GameCommand.OPEN_ORDERS.getGameBinding());
    }
}
