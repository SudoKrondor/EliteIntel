package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class FighterFireAtWillCommand extends SimpleTapCommand {
    public static final String ID = "fighter_fire_at_will";

    @Override
    public String llmDescription() {
        return "Order the deployed ship-launched fighter to fire at will and engage targets freely (open fire orders).";
    }

    public FighterFireAtWillCommand() {
        super(ID, Bindings.GameCommand.OPEN_ORDERS.getGameBinding());
    }

    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isFighterOut();
    }
}
