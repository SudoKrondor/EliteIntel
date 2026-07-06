package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class TaxiToLandingPadCommand extends SimpleTapCommand {
    public static final String ID = "taxi_to_landing_pad";

    @Override public String llmDescription() { return "Taxi the ship to the assigned landing pad."; }

    public TaxiToLandingPadCommand() {
        super(ID, Bindings.GameCommand.BINDING_SET_SPEED_ZERO.getGameBinding());
    }

    /**
     * Setting speed to zero lets the Docking Computer auto-taxi to the pad, so this only applies while
     * flying the main ship in normal space near the station - not docked, landed, or in supercruise.
     * (Requires docking authorization and a docking-computer module, which are runtime conditions not
     * derivable from Status, so they are not gated here.)
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip() && !status.isDocked() && !status.isLanded() && !status.isInSupercruise();
    }
}
