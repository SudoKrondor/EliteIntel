package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.actions.handlers.commands.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class FighterReturnToShipCommand extends SimpleTapCommand {
    public static final String ID = "fighter_return_to_ship";

    @Override
    public String llmDescription() {
        return "Order the deployed ship-launched fighter to return and dock with the mother ship.";
    }

    public FighterReturnToShipCommand() {
        super(ID, Bindings.GameCommand.BINDING_REQUEST_REQUEST_DOCK.getGameBinding());
    }

    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isFighterOut();
    }
}
