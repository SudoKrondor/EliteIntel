package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class CycleNextPanelCommand extends SimpleTapCommand {
    public static final String ID = "cycle_next_panel";

    @Override public String llmDescription() { return "Cycle to the next UI panel."; }

    public CycleNextPanelCommand() {
        super(ID, Bindings.GameCommand.BINDING_CYCLE_NEXT_PANEL.getGameBinding());
    }
}
