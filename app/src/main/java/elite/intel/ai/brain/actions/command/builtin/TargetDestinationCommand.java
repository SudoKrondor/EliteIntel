package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class TargetDestinationCommand extends SimpleTapCommand {
    public static final String ID = "target_destination";

    @Override public String llmDescription() { return "Target the current navigation destination."; }

    public TargetDestinationCommand() {
        super(ID, Bindings.GameCommand.BINDING_TARGET_NEXT_ROUTE_SYSTEM.getGameBinding());
    }
}
