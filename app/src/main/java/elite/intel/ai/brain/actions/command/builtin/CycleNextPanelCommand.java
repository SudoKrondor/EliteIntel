package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class CycleNextPanelCommand extends SimpleTapCommand {
    public CycleNextPanelCommand() {
        super(CommandIds.CYCLE_NEXT_PANEL, Bindings.GameCommand.BINDING_CYCLE_NEXT_PANEL.getGameBinding());
    }
}
