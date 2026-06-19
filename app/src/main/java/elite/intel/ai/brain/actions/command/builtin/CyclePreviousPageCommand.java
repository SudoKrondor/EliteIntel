package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class CyclePreviousPageCommand extends SimpleTapCommand {
    public static final String ID = "cycle_previous_page";

    public CyclePreviousPageCommand() {
        super(ID, Bindings.GameCommand.BINDING_CYCLE_PREVIOUS_PAGE.getGameBinding());
    }
}
