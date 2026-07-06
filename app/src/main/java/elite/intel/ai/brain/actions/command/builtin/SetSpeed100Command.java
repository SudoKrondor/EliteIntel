package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class SetSpeed100Command extends SimpleTapCommand {
    public static final String ID = "set_speed_100";

    @Override public String llmDescription() { return "Set the throttle to 100 percent."; }

    public SetSpeed100Command() {
        super(ID, Bindings.GameCommand.BINDING_SET_SPEED100.getGameBinding());
    }

    /** Ship throttle: only while piloting the main ship and not docked/landed (no throttle when stationary). */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip() && !status.isDocked() && !status.isLanded();
    }
}
