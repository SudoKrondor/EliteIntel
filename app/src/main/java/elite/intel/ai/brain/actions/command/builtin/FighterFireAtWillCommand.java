package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class FighterFireAtWillCommand extends SimpleTapCommand {
    public static final String ID = "fighter_fire_at_will";

    public FighterFireAtWillCommand() {
        super(ID, Bindings.GameCommand.OPEN_ORDERS.getGameBinding());
    }
}
