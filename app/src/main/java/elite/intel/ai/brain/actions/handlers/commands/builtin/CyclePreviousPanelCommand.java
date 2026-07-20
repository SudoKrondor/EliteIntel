package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.actions.handlers.commands.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class CyclePreviousPanelCommand extends SimpleTapCommand {
    public static final String ID = "cycle_previous_panel";

    @Override
    public String llmDescription() {
        return "Cycle to the previous panel/tab-group in the currently open in-game UI.";
    }

    public CyclePreviousPanelCommand() {
        super(ID, Bindings.GameCommand.BINDING_CYCLE_PREVIOUS_PANEL.getGameBinding());
    }

    /**
     * Available anywhere. The game interface can be opened in any state
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }
}
