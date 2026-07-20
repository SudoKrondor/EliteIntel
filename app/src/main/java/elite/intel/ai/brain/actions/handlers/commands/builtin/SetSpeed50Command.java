package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.actions.handlers.commands.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class SetSpeed50Command extends SimpleTapCommand {
    public static final String ID = "set_speed_50";

    @Override
    public String llmDescription() {
        return "Set the throttle to 50%.";
    }

    public SetSpeed50Command() {
        super(ID, Bindings.GameCommand.BINDING_SET_SPEED50.getGameBinding());
    }

    /// in any vehicle
    @Override
    public boolean isVisibleForLLM(Status status) {
        return (status.isInMainShip() || status.isInSrv() || status.isInFighter()) && (!status.isDocked() && !status.isLanded());
    }
}
