package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.brain.actions.handlers.commands.SimpleTapCommand;
import elite.intel.ai.hands.Bindings;
import elite.intel.session.Status;

/**
 * Self-describing tap command. Binding sourced from Bindings.GameCommand.
 * Dispatched directly via CommandRegistry.
 */
@RegisterCommand
public final class ActivateUiControlCommand extends SimpleTapCommand {
    public static final String ID = "activate_ui_control";

    @Override
    public String llmDescription() {
        return "Press the select/activate button on the currently focused in-game UI control (the UI 'select' key).";
    }


    public ActivateUiControlCommand() {
        super(ID, Bindings.GameCommand.BINDING_ACTIVATE.getGameBinding());
    }

    /** UI-navigation tap: usable in any interactive context; only a hyperspace jump locks out input. */
    @Override
    public boolean isVisibleForLLM(Status status) {
        return !status.isFsdJump();
    }
}
