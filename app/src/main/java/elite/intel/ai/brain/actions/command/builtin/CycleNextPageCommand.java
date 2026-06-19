package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class CycleNextPageCommand extends SimpleTapCommand {
    public CycleNextPageCommand() {
        super(CommandIds.CYCLE_NEXT_PAGE, Bindings.GameCommand.BINDING_CYCLE_NEXT_PAGE.getGameBinding());
    }
}
