package elite.intel.gameapi.inputs;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.CommandOutcome;
import elite.intel.ai.hands.KeyProcessor;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.db.managers.ShipRouteManager;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.eventbus.GameEventBus;
import elite.intel.session.ui.UINavigator;
import elite.intel.util.AudioPlayer;
import elite.intel.util.PlayBeepEvent;
import elite.intel.util.StringUtls;

import static elite.intel.ai.hands.Bindings.GameCommand.*;

public class RoutePlotter {


    private final UINavigator navigator = new UINavigator();

    public RoutePlotter() {
    }

    /**
     * Plots an in-game route to {@code destination} and reports its outcome instead of self-narrating, so
     * the calling command can fold it into its own {@link CommandOutcome} (this helper is shared by many
     * navigation commands). Returns the "already plotted" outcome when the route is unchanged; returns
     * {@code null} when it actually plots (the side-effect input sequence + beep) or when there is no
     * destination - in those cases the caller supplies its own outcome.
     */
    public JsonObject plotRoute(String destination) {
        navigator.closeOpenPanel();
        if (destination == null || destination.isEmpty()) {
            return null;
        }

        String finalDestination = ShipRouteManager.getInstance().getDestination();
        if (finalDestination != null && finalDestination.equalsIgnoreCase(destination)) {
            return CommandOutcome.speak(StringUtls.localizedLlm("handler.route.alreadyPlotted", finalDestination));
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

        GameEventBus.publish(new PlayBeepEvent(AudioPlayer.BEEP_2));
        return null;
    }
}
