package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class FighterHoldFireCommand extends SimpleTapCommand {
    public static final String ID = "fighter_hold_fire";

    @Override public String llmDescription() { return "Order the ship-launched fighter to hold fire."; }

    public FighterHoldFireCommand() {
        super(ID, Bindings.GameCommand.BINDING_REQUEST_HOLD_FIRE.getGameBinding());
    }
}
