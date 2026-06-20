package elite.intel.gameapi.inputs;

import elite.intel.ai.hands.Bindings;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.gameapi.GameControllerBus;
import elite.intel.session.Status;
import elite.intel.session.ui.UINavigator;

import static elite.intel.ai.hands.Bindings.GameCommand.*;

public class UiNavCommon {

    public static void close() {
        Status status = Status.getInstance();
        if (status.isSystemMapOpen()) {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_LOCAL_MAP.getGameBinding())));
        }
        if (status.isGalaxyMapOpen()) {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_GALAXY_MAP.getGameBinding())));
        }
        new UINavigator().closeOpenPanel();
        GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(BINDING_EXIT_KEY.getGameBinding())));
    }


    public static void prepToKnownUiPositionWhileInTheShipAtStation() {
        GameControllerBus.publish(GameInputSequenceEvent.of(
                GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_DOWN.getGameBinding()),
                GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_DOWN.getGameBinding()),
                GameInputStep.bindingTap(Bindings.GameCommand.BINDING_UI_DOWN.getGameBinding())
        ));
    }
}
