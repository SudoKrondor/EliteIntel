package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class TaxiToLandingPadCommand extends SimpleTapCommand {
    public TaxiToLandingPadCommand() {
        super(CommandIds.TAXI_TO_LANDING_PAD, Bindings.GameCommand.BINDING_SET_SPEED_ZERO.getGameBinding());
    }
}
