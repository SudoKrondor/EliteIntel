package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.session.Status;
import elite.intel.util.ClipboardUtils;

/**
 * Self-describing "navigate from memory" command.
 * Owns its own execution: body migrated 1:1 from the legacy PasteFromMemoryHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class NavigateFromMemoryCommand implements IntelCommand {
    public static final String ID = "navigate_from_memory";

    @Override
    public String llmDescription() {
        return "Plot a route to the system name currently on the clipboard (paste-from-memory navigation).";
    }


    @Override
    public String id() {
        return ID;
    }

    /// Navigation is avialable anywhere in the game
    @Override
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        RoutePlotter plotter = new RoutePlotter();
        return plotter.plotRoute(ClipboardUtils.getClipboardText());
    }
}
