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
        return "Plot a route to the star system name the commander copied to the clipboard from a website such as "
                + "INARA or Spansh (paste-from-memory navigation). Never for a system the commander names aloud.";
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
