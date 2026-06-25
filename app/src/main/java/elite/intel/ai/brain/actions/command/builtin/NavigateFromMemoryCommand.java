package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.gameapi.inputs.RoutePlotter;
import elite.intel.util.ClipboardUtils;

/**
 * Self-describing "navigate from memory" command.
 * Owns its own execution: body migrated 1:1 from the legacy PasteFromMemoryHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class NavigateFromMemoryCommand implements IntelCommand {
    public static final String ID = "navigate_from_memory";

    @Override public String llmDescription() { return "Plot a route to a previously remembered location."; }


    @Override
    public String id() {
        return ID;
    }

    @Override
    public JsonObject execute(JsonObject params, String responseText) {
        RoutePlotter plotter = new RoutePlotter();
        return plotter.plotRoute(ClipboardUtils.getClipboardText());
    }
}
