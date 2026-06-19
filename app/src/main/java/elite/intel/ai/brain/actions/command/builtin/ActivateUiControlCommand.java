package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

/**
 * Self-describing tap command. Binding sourced from Bindings.GameCommand.
 * Dispatched directly via CommandRegistry.
 */
@RegisterCommand
public final class ActivateUiControlCommand extends SimpleTapCommand {

    public ActivateUiControlCommand() {
        super(CommandIds.ACTIVATE_UI_CONTROL, Bindings.GameCommand.BINDING_ACTIVATE.getGameBinding());
    }
}
