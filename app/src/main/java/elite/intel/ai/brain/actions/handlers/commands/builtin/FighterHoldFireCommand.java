package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.actions.handlers.commands.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class FighterHoldFireCommand extends SimpleTapCommand {
    public static final String ID = "fighter_hold_fire";

    @Override
    public String llmDescription() {
        return "Order the deployed ship-launched fighter to hold fire / cease fire.";
    }

    public FighterHoldFireCommand() {
        super(ID, Bindings.GameCommand.BINDING_REQUEST_HOLD_FIRE.getGameBinding());
    }

    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isFighterOut();
    }
}
