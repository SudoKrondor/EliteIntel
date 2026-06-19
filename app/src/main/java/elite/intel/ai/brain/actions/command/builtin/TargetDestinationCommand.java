package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class TargetDestinationCommand extends SimpleTapCommand {
    public TargetDestinationCommand() {
        super(CommandIds.TARGET_DESTINATION, Bindings.GameCommand.BINDING_TARGET_NEXT_ROUTE_SYSTEM.getGameBinding());
    }
}
