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

import java.util.ArrayList;
import java.util.List;

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
     * <p>
     * WHY 1000 and not the 3000 the whole step used to be: the wait above now absorbs the part that used to
     * need guessing - how long the machine takes to report the map open at all - so this only has to cover
     * the fly-in behind that signal. Raised to 3000 on 2026-09-01 while chasing a commander whose search box
     * never got focus; it was not the cause (their HOTAS was sending UI_Left continuously, see
     * UI_* axis-half bindings) and the two seconds are dead air before the companion speaks, since the
     * handler does not return its answer until this whole sequence finishes. Do not raise it on a hunch.
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
        // WHY this is read before a single key is dispatched: the galaxy map binding is a TOGGLE, so every
        // decision below turns on whether the map is already up. Reading it after closeOpenPanel() would be
        // reading it after the very keystrokes that are meant to change it.
        boolean mapAlreadyOpen = status.isGalaxyMapOpen();

        // An open galaxy map is shut by the sequence below, with its own binding, rather than here - see the
        // note on that step. Every other panel is closed exactly as it always was.
        if (!mapAlreadyOpen) {
            navigator.closeOpenPanel();
        }
        if (destination == null || destination.isEmpty()) {
            return null;
        }

        String finalDestination = ShipRouteManager.getInstance().getDestination();
        if (finalDestination != null && finalDestination.equalsIgnoreCase(destination)) {
            return StringUtls.localizedResponse("handler.route.alreadyPlotted", finalDestination);
        }


        List<GameInputStep> steps = new ArrayList<>();
        // WHY a map the commander already had open is SHUT and opened again rather than simply used.
        //
        // The steps below - zoom in, left, right, select - only land on the search box from a map that has
        // just opened, because that is the only state whose UI focus is known. A map the commander has been
        // flying around in has focus wherever they left it, so the same keystrokes go somewhere else and the
        // system name is typed into nothing: silently, with every keystroke reporting success and the
        // companion still announcing the course it never plotted (commander bundle 2026-09-01 - journal
        // "Music: GalaxyMap" and GuiFocus 6 one second before the command, no NavRoute afterwards).
        //
        // It has to be the map's own binding that shuts it. UI_Back does not close the galaxy map, which is
        // why closeOpenPanel() is skipped here entirely: with autoExitUiBeforeOpeningAnotherWindow on it
        // would fire ten of them at a map that ignores them, and the tap below would then be the thing that
        // closed it - leaving the sequence running against no map at all. GuiFocus is exclusive, so an open
        // galaxy map already says there is no other panel for closeOpenPanel() to shut.
        if (mapAlreadyOpen) {
            steps.add(mapToggleStep());
        }
        // WHY a wait rather than a tap straight away: the keys that shut the open map - either the toggle
        // above or closeOpenPanel() before it - have been dispatched, but the game reports GuiFocus through
        // Status.json a beat later. The binding is a toggle, so tapping it against a stale "still open"
        // reading would close the very map this sequence is trying to open.
        steps.add(GameInputStep.waitUntil("galaxy map closed", () -> !status.isGalaxyMapOpen(), GALAXY_MAP_CLOSE_TIMEOUT_MS));
        steps.add(mapToggleStep());
        // WHY this replaced a blind delay(3000): on slower hardware the map itself took two seconds to
        // appear (commander bundle 2026-08-23 - map binding at 16:07:39Z, journal "Music: GalaxyMap" at
        // 16:07:41Z), so barely a second of the budget was left. Every step below then ran against a map
        // that was not taking input yet: the search field never got focus and the system name was typed
        // into nothing. Watching the game's own state costs nothing on a fast machine and does not
        // guess on a slow one.
        steps.add(GameInputStep.waitUntil("galaxy map open", status::isGalaxyMapOpen, GALAXY_MAP_OPEN_TIMEOUT_MS));
        steps.addAll(List.of(
                // WHY a settle on top of the confirmed signal: GuiFocus flips when the map starts opening,
                // not when its fly-in has finished and it accepts UI input. The wait above removes the
                // guesswork about how long the machine takes to get there; this is the fixed part that is left.
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

        GameControllerBus.publish(new GameInputSequenceEvent(steps));

        GameEventBus.publish(new PlayBeepEvent(AudioPlayer.BEEP_2));
        return null;
    }

    /**
     * Taps the galaxy map binding for wherever the commander is standing - the map has a separate binding on
     * foot and in an SRV, and the ship one does nothing there.
     * <p>
     * Named for what the key is rather than what it does: it is a toggle, so the same step opens a shut map
     * and shuts an open one. Both readings are used above, in that order.
     */
    private GameInputStep mapToggleStep() {
        if (status.isOnFoot()) {
            return GameInputStep.bindingTap(BINDING_GALAXY_MAP_HUMANOID.getGameBinding());
        }
        if (status.isInSrv()) {
            return GameInputStep.bindingTap(BINDING_GALAXY_MAP_BUGGY.getGameBinding());
        }
        return GameInputStep.bindingTap(BINDING_GALAXY_MAP.getGameBinding());
    }
}
