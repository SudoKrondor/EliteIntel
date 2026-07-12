package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class FighterDefendCommand extends SimpleTapCommand {
    public static final String ID = "fighter_defend";

    @Override
    public String llmDescription() {
        return "Order the deployed ship-launched fighter to defend the mother ship.";
    }

    public FighterDefendCommand() {
        super(ID, Bindings.GameCommand.BINDING_REQUEST_DEFENSIVE_BEHAVIOUR.getGameBinding());
    }

    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isFighterOut();
    }
}
