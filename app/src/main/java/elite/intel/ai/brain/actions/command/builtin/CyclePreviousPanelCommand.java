package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

@RegisterCommand
public final class CyclePreviousPanelCommand extends SimpleTapCommand {
    public CyclePreviousPanelCommand() {
        super(CommandIds.CYCLE_PREVIOUS_PANEL, Bindings.GameCommand.BINDING_CYCLE_PREVIOUS_PANEL.getGameBinding());
    }
}
