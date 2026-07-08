package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class FighterFireAtWillCommand extends SimpleTapCommand {
    public static final String ID = "fighter_fire_at_will";

    @Override public String llmDescription() { return "Use this when the commander orders the ship-launched fighter to fire at will or attack targets freely."; }

    public FighterFireAtWillCommand() {
        super(ID, Bindings.GameCommand.OPEN_ORDERS.getGameBinding());
    }

    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isFighterOut();
    }
}
