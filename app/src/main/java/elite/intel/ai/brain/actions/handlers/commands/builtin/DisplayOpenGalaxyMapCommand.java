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
 * Stage-4b self-describing command for "open galaxy map".
 */
@RegisterCommand
public final class DisplayOpenGalaxyMapCommand implements IntelCommand {
    public static final String ID = "display_open_galaxy_map";

    @Override public String llmDescription() { return "Open the galaxy map."; }


    private final UINavigator navigator = new UINavigator();

    @Override
    public String id() {
        return ID;
    }

    @Override
    ///available anywhere
    public boolean isVisibleForLLM(Status status) {
        return true;
    }

    @Override
    public String execute(JsonObject params, String responseText) {
        navigator.closeOpenPanel();
        // Context-correct binding, and the ship one as the fallback: the old chain tapped nothing at all
        // in any state that was neither ship, fighter, SRV nor on foot.
        GameControllerBus.publish(GameInputSequenceEvent.single(UiNavCommon.galaxyMapToggleStep()));
        return null;
    }
}
