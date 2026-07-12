package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

@RegisterCommand
public final class CycleNextPanelCommand extends SimpleTapCommand {
    public static final String ID = "cycle_next_panel";

    @Override
    public String llmDescription() {
        return "Cycle to the next panel/tab-group in the currently open in-game UI.";
    }

    public CycleNextPanelCommand() {
        super(ID, Bindings.GameCommand.BINDING_CYCLE_NEXT_PANEL.getGameBinding());
    }

    /**
     * Available anywhere. The game interface can be opened in any state
     */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }
}
