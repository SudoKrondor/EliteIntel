package elite.intel.ai.brain.actions.command.builtin;

import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.brain.actions.command.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;

/**
 * Self-describing tap command. Binding sourced from Bindings.GameCommand.
 * Dispatched directly via CommandRegistry.
 */
@RegisterCommand
public final class ActivateUiControlCommand extends SimpleTapCommand {
    public static final String ID = "activate_ui_control";


    public ActivateUiControlCommand() {
        super(ID, Bindings.GameCommand.BINDING_ACTIVATE.getGameBinding());
    }
}
