package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class FighterReturnToShipCommand extends SimpleTapCommand {
    public static final String ID = "fighter_return_to_ship";

    @Override public String llmDescription() { return "Order the ship-launched fighter to return to the ship."; }

    public FighterReturnToShipCommand() {
        super(ID, Bindings.GameCommand.BINDING_REQUEST_REQUEST_DOCK.getGameBinding());
    }
}
