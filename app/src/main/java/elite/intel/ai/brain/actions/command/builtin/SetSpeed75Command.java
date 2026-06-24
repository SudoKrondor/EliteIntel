package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class SetSpeed75Command extends SimpleTapCommand {
    public static final String ID = "set_speed_75";

    @Override public String llmDescription() { return "Set the throttle to 75 percent."; }

    public SetSpeed75Command() {
        super(ID, Bindings.GameCommand.BINDING_SET_SPEED75.getGameBinding());
    }
}
