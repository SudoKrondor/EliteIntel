package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class TargetDestinationCommand extends SimpleTapCommand {
    public static final String ID = "target_destination";

    @Override
    public String llmDescription() {
        return "Select and target the next system on the plotted route as the FSD jump destination.";
    }

    /** Route-target selection is a main-ship cockpit function. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return status.isInMainShip();
    }

    public TargetDestinationCommand() {
        super(ID, Bindings.GameCommand.BINDING_TARGET_NEXT_ROUTE_SYSTEM.getGameBinding());
    }
}
