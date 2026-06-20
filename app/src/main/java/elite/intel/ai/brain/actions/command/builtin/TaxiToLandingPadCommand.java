package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class TaxiToLandingPadCommand extends SimpleTapCommand {
    public static final String ID = "taxi_to_landing_pad";

    public TaxiToLandingPadCommand() {
        super(ID, Bindings.GameCommand.BINDING_SET_SPEED_ZERO.getGameBinding());
    }
}
