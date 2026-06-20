package elite.intel.gameapi.inputs;

import elite.intel.ai.hands.KeyProcessor;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.db.managers.ShipRouteManager;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.GameControllerBus;
import elite.intel.session.ui.UINavigator;
import elite.intel.util.AudioPlayer;
import elite.intel.util.StringUtls;

import static elite.intel.ai.hands.Bindings.GameCommand.*;

public class RoutePlotter {


    private final UINavigator navigator = new UINavigator();

    public RoutePlotter() {
    }

    public void plotRoute(String destination) {
        navigator.closeOpenPanel();
        if (destination == null || destination.isEmpty()) {
            return;
        }

        String finalDestination = ShipRouteManager.getInstance().getDestination();
        if (finalDestination != null && finalDestination.equalsIgnoreCase(destination)) {
            EventBusManager.publish(new AiVoxResponseEvent(StringUtls.localizedLlm("handler.route.alreadyPlotted", finalDestination)));
            return;
        }

        GameControllerBus.publish(GameInputSequenceEvent.of(
                GameInputStep.bindingTap(BINDING_GALAXY_MAP.getGameBinding()),
                GameInputStep.delay(3000),
                GameInputStep.bindingHold(BINDING_CAM_ZOOM_IN.getGameBinding(), 500),
                GameInputStep.bindingTap(BINDING_UI_LEFT.getGameBinding()),
                GameInputStep.delay(200),
                GameInputStep.bindingTap(BINDING_UI_RIGHT.getGameBinding()),
                GameInputStep.delay(200),
                GameInputStep.bindingTap(BINDING_ACTIVATE.getGameBinding()),
                GameInputStep.delay(200),
                GameInputStep.text(destination),
                GameInputStep.delay(250),
                GameInputStep.rawKey(KeyProcessor.KEY_DOWNARROW),
                GameInputStep.rawKey(KeyProcessor.KEY_ENTER),
                GameInputStep.delay(1000),
                GameInputStep.rawKey(KeyProcessor.KEY_ENTER)
        ));

        AudioPlayer.getInstance().playBeep(AudioPlayer.BEEP_2);
    }
}
