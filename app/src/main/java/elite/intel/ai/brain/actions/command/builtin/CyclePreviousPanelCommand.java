package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class CyclePreviousPanelCommand extends SimpleTapCommand {
    public static final String ID = "cycle_previous_panel";

    @Override public String llmDescription() { return "Cycle to the previous UI panel."; }

    public CyclePreviousPanelCommand() {
        super(ID, Bindings.GameCommand.BINDING_CYCLE_PREVIOUS_PANEL.getGameBinding());
    }
}
