package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class CyclePreviousPageCommand extends SimpleTapCommand {
    public CyclePreviousPageCommand() {
        super(CommandIds.CYCLE_PREVIOUS_PAGE, Bindings.GameCommand.BINDING_CYCLE_PREVIOUS_PAGE.getGameBinding());
    }
}
