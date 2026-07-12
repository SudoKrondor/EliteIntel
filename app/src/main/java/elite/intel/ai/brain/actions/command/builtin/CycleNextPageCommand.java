package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class CycleNextPageCommand extends SimpleTapCommand {
    public static final String ID = "cycle_next_page";

    @Override
    public String llmDescription() {
        return "Cycle to the next tab/page within the currently open in-game panel.";
    }

    public CycleNextPageCommand() {
        super(ID, Bindings.GameCommand.BINDING_CYCLE_NEXT_PAGE.getGameBinding());
    }

    /**
     * Available anywhere. The game interface can be opened in any state
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }
}
