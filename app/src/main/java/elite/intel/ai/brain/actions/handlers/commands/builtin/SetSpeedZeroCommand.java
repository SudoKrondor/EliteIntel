package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.actions.handlers.commands.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class SetSpeedZeroCommand extends SimpleTapCommand {
    public static final String ID = "set_speed_to_zero_0_stop_ship";

    @Override
    public String llmDescription() {
        return "Set the throttle to zero to stop the ship (cut all forward thrust).";
    }

    public SetSpeedZeroCommand() {
        super(ID, Bindings.GameCommand.BINDING_SET_SPEED_ZERO.getGameBinding());
    }

    /// in any vehicle
    @Override
    public boolean isVisibleForLLM(Status status) {
        return (status.isInMainShip() || status.isInSrv() || status.isInFighter()) && (!status.isDocked() && !status.isLanded());
    }
}
