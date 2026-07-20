package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.actions.handlers.commands.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class SetSpeed75Command extends SimpleTapCommand {
    public static final String ID = "set_speed_75";

    @Override
    public String llmDescription() {
        return "Set the throttle to 75%.";
    }

    public SetSpeed75Command() {
        super(ID, Bindings.GameCommand.BINDING_SET_SPEED75.getGameBinding());
    }

    /// in any vehicle
    @Override
    public boolean isVisibleForLLM(Status status) {
        return (status.isInMainShip() || status.isInSrv() || status.isInFighter()) && (!status.isDocked() && !status.isLanded());
    }
}
