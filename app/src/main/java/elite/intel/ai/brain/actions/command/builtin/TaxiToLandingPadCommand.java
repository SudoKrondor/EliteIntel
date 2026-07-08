package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class TaxiToLandingPadCommand extends SimpleTapCommand {
    public static final String ID = "taxi_to_landing_pad";

    @Override
    public String llmDescription() {
        return "Engage the docking computer to automatically taxi/dock the ship to the assigned landing pad.";
    }

    public TaxiToLandingPadCommand() {
        super(ID, Bindings.GameCommand.BINDING_SET_SPEED_ZERO.getGameBinding());
    }

    ///
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip() && status.isInSupercruise();
    }
}
