package elite.intel.ai.brain.actions.command.builtin;
import elite.intel.ai.brain.actions.command.CommandIds;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.RoutePlotter;
import elite.intel.util.ClipboardUtils;

/**
 * Self-describing "navigate from memory" command.
 * Owns its own execution: body migrated 1:1 from the legacy PasteFromMemoryHandler,
 * routed through CommandRegistry via the self-describing model.
 */
@RegisterCommand
public final class NavigateFromMemoryCommand implements IntelCommand {

    @Override
    public String id() {
        return CommandIds.NAVIGATE_FROM_MEMORY;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        RoutePlotter plotter = new RoutePlotter();
        plotter.plotRoute(ClipboardUtils.getClipboardText());
    }
}
