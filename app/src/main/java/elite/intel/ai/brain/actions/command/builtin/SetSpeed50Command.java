package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class SetSpeed50Command extends SimpleTapCommand {
    public static final String ID = "set_speed_50";

    @Override public String llmDescription() { return "Set the throttle to 50 percent."; }

    public SetSpeed50Command() {
        super(ID, Bindings.GameCommand.BINDING_SET_SPEED50.getGameBinding());
    }

    /** Ship throttle: only while piloting the main ship and not docked/landed (no throttle when stationary). */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip() && !status.isDocked() && !status.isLanded();
    }
}
