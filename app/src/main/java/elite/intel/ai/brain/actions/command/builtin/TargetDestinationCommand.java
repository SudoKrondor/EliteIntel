package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class TargetDestinationCommand extends SimpleTapCommand {
    public static final String ID = "target_destination";

    @Override public String llmDescription() { return "Target the current navigation destination."; }

    public TargetDestinationCommand() {
        super(ID, Bindings.GameCommand.BINDING_TARGET_NEXT_ROUTE_SYSTEM.getGameBinding());
    }

    /** Route-target selection is a main-ship cockpit function. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip();
    }
}
