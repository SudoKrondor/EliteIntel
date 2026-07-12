package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class CyclePreviousPageCommand extends SimpleTapCommand {
    public static final String ID = "cycle_previous_page";

    @Override
    public String llmDescription() {
        return "Cycle to the previous tab/page within the currently open in-game panel.";
    }

    public CyclePreviousPageCommand() {
        super(ID, Bindings.GameCommand.BINDING_CYCLE_PREVIOUS_PAGE.getGameBinding());
    }

    /**
     * Available anywhere. The game interface can be opened in any state
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }
}
