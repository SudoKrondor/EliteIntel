package elite.intel.ai.brain.actions.command.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.command.RegisterCommand;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.session.Status;
import elite.intel.session.ui.UINavigator;

import static elite.intel.ai.hands.Bindings.GameCommand.*;

/**
 * Stage-4b self-describing command for "open system map".
 */
@RegisterCommand
public final class DisplayOpenSystemMapCommand implements IntelCommand {
    public static final String ID = "display_open_system_map";

    @Override public String llmDescription() { return "Open the system map."; }


    private final UINavigator navigator = new UINavigator();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void execute(JsonObject params, String responseText) {
        navigator.closeOpenPanel();
        Status status = Status.getInstance();
        if (status.isInMainShip() || status.isInFighter()) {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_LOCAL_MAP.getGameBinding())));
        }

        if (status.isInSrv()) {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_LOCAL_MAP_BUGGY.getGameBinding())));
        }

        if (status.isOnFoot()) {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_SYSTEM_MAP_HUMANOID.getGameBinding())));
        }
    }
}
