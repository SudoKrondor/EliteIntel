package elite.intel.gameapi.inputs;

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_ACTIVATE;
import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_CAM_ZOOM_IN;
import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_GALAXY_MAP;
import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_GALAXY_MAP_BUGGY;
import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_GALAXY_MAP_HUMANOID;
import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_UI_DOWN;
import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_UI_LEFT;
import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_UI_RIGHT;
import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_UI_SELECT;
import elite.intel.ai.hands.KeyProcessor;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.db.managers.ShipRouteManager;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.eventbus.GameEventBus;
import elite.intel.session.Status;
import elite.intel.session.ui.UINavigator;
import elite.intel.util.AudioPlayer;
import elite.intel.util.PlayBeepEvent;
import elite.intel.util.StringUtls;

public class RoutePlotter {

    private final UINavigator navigator = new UINavigator();
    private final Status status = Status.getInstance();

    public RoutePlotter() {
    }

    public String plotRoute(String destination) {
        navigator.closeOpenPanel();

        if (destination == null || destination.isEmpty()) {
            return null;
        }

        String finalDestination = ShipRouteManager.getInstance().getDestination();
        if (finalDestination != null && finalDestination.equalsIgnoreCase(destination)) {
            return StringUtls.localizedResponse("handler.route.alreadyPlotted", finalDestination);
        }

        GameInputStep gameInputStep;

        if (status.isOnFoot()) {
            gameInputStep = GameInputStep.bindingTap(BINDING_GALAXY_MAP_HUMANOID.getGameBinding());
        } else if (status.isInSrv()) {
            gameInputStep = GameInputStep.bindingTap(BINDING_GALAXY_MAP_BUGGY.getGameBinding());
        } else {
            gameInputStep = GameInputStep.bindingTap(BINDING_GALAXY_MAP.getGameBinding());
        }

        GameControllerBus.publish(GameInputSequenceEvent.of(
                gameInputStep,
                GameInputStep.delay(3000),

                // Establish a predictable Galaxy Map UI state.
                GameInputStep.bindingHold(BINDING_CAM_ZOOM_IN.getGameBinding(), 500),
                GameInputStep.bindingTap(BINDING_UI_LEFT.getGameBinding()),
                GameInputStep.delay(200),
                GameInputStep.bindingTap(BINDING_UI_RIGHT.getGameBinding()),
                GameInputStep.delay(200),

                // Activate the search field and enter the destination.
                GameInputStep.bindingTap(BINDING_ACTIVATE.getGameBinding()),
                GameInputStep.delay(200),
                GameInputStep.text(destination),
                GameInputStep.delay(250),
                // WHY a raw arrow rather than the UI_Down binding: this is the one step that has to
                // move focus OUT of the search field, and a focused Elite text field takes keystrokes
                // as text - it never consults the UI_* bindings. A commander who rebound UI navigation
                // to a chord (Ctrl+S, Shift+S) therefore had the chord typed into the box instead:
                // focus stayed put and every step after it was typed too. The arrow is what the field
                // itself honours, and it is the stock UI_Down key, so this is a no-op for everyone
                // whose bindings are near-default. Once focus is on the result list the text field is
                // out of the way and the steps below are binding-driven again, as they should be.
                GameInputStep.rawKey(KeyProcessor.KEY_DOWNARROW, 0, 0),
                GameInputStep.delay(200),
                
                // Select the system and allow the Galaxy Map to centre on it.
                GameInputStep.bindingTap(BINDING_UI_SELECT.getGameBinding()),
                GameInputStep.delay(1200),

                // A small camera movement transitions the Galaxy Map into a state
                // where the right-hand system actions can be navigated.
                GameInputStep.bindingTap(BINDING_CAM_ZOOM_IN.getGameBinding()),
                GameInputStep.delay(200),

                // Move into the right-hand system actions menu.
                GameInputStep.bindingTap(BINDING_UI_RIGHT.getGameBinding()),

                // Navigate to Plot Route.
                GameInputStep.bindingTap(BINDING_UI_DOWN.getGameBinding()),
                GameInputStep.bindingTap(BINDING_UI_DOWN.getGameBinding()),
                GameInputStep.bindingTap(BINDING_UI_DOWN.getGameBinding()),
                GameInputStep.bindingTap(BINDING_UI_DOWN.getGameBinding()),
                GameInputStep.bindingTap(BINDING_UI_DOWN.getGameBinding()),
                GameInputStep.bindingTap(BINDING_UI_DOWN.getGameBinding()),
                GameInputStep.bindingTap(BINDING_UI_DOWN.getGameBinding()),

                // Activate Plot Route.
                GameInputStep.bindingTap(BINDING_UI_SELECT.getGameBinding())
        ));

        GameEventBus.publish(new PlayBeepEvent(AudioPlayer.BEEP_2));
        return null;
    }
}