package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.actions.handlers.commands.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.gameapi.inputs.UiNavCommon;
import elite.intel.session.Status;
import elite.intel.session.ui.UINavigator;


/**
 * Stage-4b self-describing command for "open system map".
 */
@RegisterCommand
public final class DisplayOpenSystemMapCommand implements IntelCommand {
    public static final String ID = "display_open_system_map";

    @Override
    public String llmDescription() {
        return "Open the system (local) map.";
    }


    private final UINavigator navigator = new UINavigator();

    @Override
    public String id() {
        return ID;
    }

    @Override
    ///available everywhere
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        navigator.closeOpenPanel();
        // See DisplayOpenGalaxyMapCommand: one context rule, and the ship binding as the fallback.
        GameControllerBus.publish(GameInputSequenceEvent.single(UiNavCommon.systemMapToggleStep()));
        return null;
    }
}
