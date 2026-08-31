package elite.intel.gameapi.inputs;

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

import static elite.intel.ai.hands.Bindings.GameCommand.*;

public class RoutePlotter {

    /**
     * How long to give the game to report the galaxy map shut after {@link UINavigator#closeOpenPanel()}.
     */
    private static final int GALAXY_MAP_CLOSE_TIMEOUT_MS = 2000;
    /**
     * How long to give the game to report the galaxy map open after the binding is tapped.
     */
    private static final int GALAXY_MAP_OPEN_TIMEOUT_MS = 15000;
    /**
     * Settle time between the map reporting itself open and the first keystroke aimed at it.
     */
    private static final int GALAXY_MAP_SETTLE_MS = 1000;

    private final UINavigator navigator = new UINavigator();
    private final Status status = Status.getInstance();

    public RoutePlotter() {
    }

    /**
     * Plots a route and folds whatever the plotter has to say into the caller's own answer.
     * <p>
     * {@link #plotRoute} returns a sentence precisely when it did NOT plot a new route - the commander is
     * already flying there, or named nowhere. Every caller that dropped that on the floor produced the same
     * silent failure: an answer naming a destination, no galaxy map opening, and a commander with no way to
     * tell a working search from a broken one.
     *
     * @param answer      what the command was going to say regardless
     * @param destination the system to plot to
     */
    public String plotRouteAnd(String answer, String destination) {
        String note = plotRoute(destination);
        if (note == null || note.isBlank()) return answer;
        if (answer == null || answer.isBlank()) return note;
        return answer + " " + note;
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
                // WHY a wait rather than a tap straight away: closeOpenPanel() above has already dispatched the
                // keys that shut any open map, but the game reports GuiFocus through Status.json a beat later.
                // The map binding is a toggle, so tapping it against a stale "still open" reading would close
                // the very map this sequence is trying to open.
                GameInputStep.waitUntil("galaxy map closed", () -> !status.isGalaxyMapOpen(), GALAXY_MAP_CLOSE_TIMEOUT_MS),
                gameInputStep,
                // WHY this replaced a blind delay(3000): on slower hardware the map itself took two seconds to
                // appear (commander bundle 2026-08-23 - map binding at 16:07:39Z, journal "Music: GalaxyMap" at
                // 16:07:41Z), so barely a second of the budget was left. Every step below then ran against a map
                // that was not taking input yet: the search field never got focus and the system name was typed
                // into nothing. Watching the game's own state costs nothing on a fast machine and does not
                // guess on a slow one.
                GameInputStep.waitUntil("galaxy map open", status::isGalaxyMapOpen, GALAXY_MAP_OPEN_TIMEOUT_MS),
                // WHY a settle on top of the confirmed signal: GuiFocus flips when the map starts opening, not
                // when its fly-in has finished and it accepts UI input. The wait above removes the guesswork
                // about how long the machine takes to get there; this is the fixed part that is left.
                GameInputStep.delay(GALAXY_MAP_SETTLE_MS),
                GameInputStep.bindingHold(BINDING_CAM_ZOOM_IN.getGameBinding(), 500),
                GameInputStep.bindingTap(BINDING_UI_LEFT.getGameBinding()),
                GameInputStep.delay(200),
                GameInputStep.bindingTap(BINDING_UI_RIGHT.getGameBinding()),
                GameInputStep.delay(200),
                GameInputStep.bindingTap(BINDING_ACTIVATE.getGameBinding()),
                GameInputStep.delay(200),
                GameInputStep.text(destination),
                GameInputStep.delay(250),
                // WHY a raw arrow rather than the UI_Down binding, and why this is the ONE step that
                // breaks the binding-driven rule the rest of this sequence follows.
                //
                // The step has to move focus OUT of the search field. A focused Elite text field
                // consumes any keystroke that produces a PRINTABLE CHARACTER and never gets as far as
                // the UI_* bindings - so UI_Down on a bare letter (S) or on Shift+letter (Shift+S)
                // types that letter after the system name and focus never leaves the box. Every step
                // below is then typed too, and no route is plotted: silently, with every keystroke
                // reporting success. A non-printing chord (Ctrl+W/A/S/D, verified in game 2026-08-31)
                // does reach the binding, which is why UI_Down appears to work for some commanders and
                // not others - the modifier, not the binding, is what decides it.
                //
                // The Down arrow produces no character on any layout, so it is the only key that is
                // safe to send here regardless of what the commander bound. It is also the stock
                // UI_Down key, so this is a no-op for anyone near default.
                //
                // This does NOT rescue a commander who moved UI navigation off the arrows entirely
                // (bundle 2026-08-31: W/A/S/D for the UI, arrows given to power distribution). Nothing
                // we can send rescues that - it is a bindings problem, and KeyBindCheck warns about it
                // on every start rather than this line trying to guess a key on their behalf.
                GameInputStep.rawKey(KeyProcessor.KEY_DOWNARROW, 0, 0),
                GameInputStep.bindingTap(BINDING_UI_SELECT.getGameBinding()),
                GameInputStep.delay(1000),
                GameInputStep.bindingTap(BINDING_UI_SELECT.getGameBinding())
        ));

        GameEventBus.publish(new PlayBeepEvent(AudioPlayer.BEEP_2));
        return null;
    }
}
