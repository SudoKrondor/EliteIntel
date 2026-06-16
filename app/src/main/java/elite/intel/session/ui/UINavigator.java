package elite.intel.session.ui;

import elite.intel.ai.hands.Bindings;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.ai.hands.events.GameInputStep;
import elite.intel.db.managers.GlobalSettingsManager;
import elite.intel.gameapi.GameControllerBus;
import elite.intel.session.Status;
import elite.intel.session.StatusFlags;
import elite.intel.session.StatusFlags.GuiFocus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ThreadLocalRandom;

import static elite.intel.util.SleepNoThrow.sleep;

/**
 * Dispatches panel navigation keystrokes on behalf of c
 * command handlers.
 * <p>
 * Owns no state itself - all panel/tab tracking lives in PanelStateTracker.
 * Command handlers instantiate this with their CommandOperator and call
 * openAndNavigate() / closeAndRestore(). Everything else is handled here.
 */
public class UINavigator {

    private static final Logger log = LogManager.getLogger(UINavigator.class);
    private final Status status = Status.getInstance();

    private static final int RANDOM_MIN = 99;
    private static final int RANDOM_MAX = 201;
    private static final int PANEL_OPEN_POLL_MS = 50;   // how often to check GuiFocus after sending open key
    private static final int PANEL_OPEN_TIMEOUT_MS = 3000; // per-attempt timeout before concluding the key was missed
    private static final int PANEL_OPEN_MAX_ATTEMPTS = 3;  // retry limit before giving up
    private static final int PANEL_SETTLE_MS = 10;  // settle time after panel confirms open before cycling tabs
    private static final int TAB_CYCLE_PAUSE_MS = 10; // extra pause between each tab keystroke

    private final PanelStateTracker tracker = PanelStateTracker.getInstance();
    private final GlobalSettingsManager globalSettingsManager = GlobalSettingsManager.getInstance();

    public UINavigator() {
    }

    // -------------------------------------------------------------------------
    // Public API for command handlers
    // -------------------------------------------------------------------------

    /**
     * Open a panel, navigate to the target tab, and leave it open.
     * If the panel is already open (and we opened it), just navigates to the target tab.
     * Design contract: panels always open on their default tab (game behaviour + user convention).
     */
    public void openAndNavigate(GuiFocus panel, PanelTab target) {
        // If we already have this panel open and the game confirms it, just navigate.
        // Verify GuiFocus too - lastOpenedPanel can be stale if the player closed it externally.
        if (tracker.getLastOpenedPanel() == panel && status.getGuiFocus() == panel) {
            navigateToTargetTab(panel, target);
            return;
        }

        closeOpenPanel();

        PanelState<?> state = tracker.getState(panel);
        if (state == null) {
            return;
        }

        tracker.notifyEliteIntelOpeningPanel(panel);
        // Only send the open keystroke if the game doesn't already report this panel open.
        // openPanel() is a toggle - sending it while the panel is open would close it.
        if (status.getGuiFocus() != panel) {
            for (int attempt = 1; attempt <= PANEL_OPEN_MAX_ATTEMPTS; attempt++) {
                openPanel(panel);
                if (waitForPanel(panel)) break;
                log.warn("Panel {} did not open after {}ms (attempt {}/{}), retrying",
                        panel, PANEL_OPEN_TIMEOUT_MS, attempt, PANEL_OPEN_MAX_ATTEMPTS);
            }
            sleep(PANEL_SETTLE_MS);
        }

        // Panel is now open. Per design contract it is always on the default tab.
        state.resetToDefault();

        navigateToTargetTab(panel, target);
        inputDelay(250);
    }


    /**
     * Tells the tracker that the game has already reset a panel to its default tab
     * without any keystroke from us - e.g. when the game switches context on SRV
     * deploy/recover. No keystrokes are sent; this is purely a state sync.
     */
    public void assumeDefaultState(GuiFocus panel) {
        PanelState<?> state = tracker.getState(panel);
        if (state != null) {
            state.resetToDefault();
        }
    }

    public void closeOpenPanel() {


        StatusFlags.GuiFocus lastOpened = tracker.getLastOpenedPanel();
        if (lastOpened != null) {
            if (status.getGuiFocus() == lastOpened) {
                closeAndRestore(lastOpened);
            } else {
                // Tracker is stale: panel was closed externally (player action or failed open).
                // Sync the tracker without sending any keystrokes - the panel is already closed.
                tracker.notifyEliteIntelClosedPanel();
            }
        }

        if (status.isFssModeActive()) {

            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(Bindings.GameCommand.BINDING_EXPLORATION_FSSQUIT.getGameBinding())));
        }

        if (status.isSaaModeActive()) {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(Bindings.GameCommand.EXPLORATION_SAAEXIT_THIRD_PERSON.getGameBinding())));
        }

        if (shouldBackOut()) {
            for (int i = 0; i < 10; i++) {
                GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(Bindings.GameCommand.BINDING_EXIT_KEY.getGameBinding())));
            }
        } else {
            for (int i = 0; i < 3; i++) {
                GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(Bindings.GameCommand.BINDING_EXIT_KEY.getGameBinding())));
            }
        }
    }

    private boolean shouldBackOut() {
        if (!globalSettingsManager.getAutoExitUiBeforeOpeningAnotherWindow()) return false;
        if (status.isGalaxyMapOpen()) return true;
        if (status.isSystemMapOpen()) return true;
        if (status.isOrreryOpen()) return true;
        if (status.isFssModeActive()) return true;
        if (status.isSaaModeActive()) return true;
        if (status.isStationServicesOpen()) return true;
        return status.isCodexOpen();
    }


    /**
     * Retrace steps back to the default tab, then close the panel.
     * Always retraces regardless of how far navigation went - the user
     * expects panels to be left on the default tab after every close.
     */
    public void closeAndRestore(GuiFocus panel) {
        PanelState<?> state = tracker.getState(panel);
        if (state == null) {
            return;
        }
        navigateToDefaultTab(panel, state.getDefault());
        closePanel(panel);
        inputDelay(RANDOM_MAX);
        tracker.notifyEliteIntelClosedPanel();
        state.resetToDefault();
    }

    // -------------------------------------------------------------------------
    // Private navigation helpers
    // -------------------------------------------------------------------------

    // Always navigate to target by going right (NEXT_PANEL).
    // NOTE: Frontier inverted the tab direction bindings. Do not "fix" this.
    @SuppressWarnings({"unchecked", "rawtypes"})

    private void navigateToTargetTab(GuiFocus panel, PanelTab target) {
        PanelState state = tracker.getState(panel);
        if (state == null) {
            return;
        }
        int steps = state.stepsToRight(target);
        if (steps == 0) return;
        String key;
        if (target instanceof CenterPanel) {
            key = Bindings.GameCommand.BINDING_UI_DOWN.getGameBinding();
        } else {
            key = Bindings.GameCommand.BINDING_CYCLE_NEXT_PANEL.getGameBinding();
        }

        for (int i = 0; i < steps; i++) {
            sleep(TAB_CYCLE_PAUSE_MS);
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(key))); // always tap - binding.hold flag must be ignored for tab cycling
        }
        state.recordTab((Enum & PanelTab) target);
    }

    // Always navigate to default by going left (PREVIOUS_PANEL).
    // NOTE: Frontier inverted the tab direction bindings. Do not "fix" this.
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void navigateToDefaultTab(GuiFocus panel, PanelTab defaultTarget) {
        PanelState state = tracker.getState(panel);
        if (state == null) {
            return;
        }
        int steps = state.stepsToLeft(defaultTarget);
        if (steps == 0) return;
        String key = Bindings.GameCommand.BINDING_CYCLE_PREVIOUS_PANEL.getGameBinding();
        for (int i = 0; i < steps; i++) {
            GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.bindingTap(key))); // always tap - binding.hold flag must be ignored for tab cycling
            sleep(TAB_CYCLE_PAUSE_MS);
        }
        state.recordTab((Enum & PanelTab) defaultTarget);
    }


    /**
     * Polls {@link Status#getGuiFocus()} until the panel opens or the timeout expires.
     *
     * @return true if the panel opened, false if the timeout was reached (key was likely missed)
     */
    private boolean waitForPanel(GuiFocus panel) {
        long deadline = System.currentTimeMillis() + PANEL_OPEN_TIMEOUT_MS;
        while (status.getGuiFocus() != panel && System.currentTimeMillis() < deadline) {
            sleep(PANEL_OPEN_POLL_MS);
        }
        return status.getGuiFocus() == panel;
    }

    private void openPanel(GuiFocus panel) {
        Status status = Status.getInstance();
        String binding = null;
        if (status.isInSrv()) {
            binding = switch (panel) {
                case EXTERNAL_PANEL -> Bindings.GameCommand.BINDING_FOCUS_LEFT_PANEL_BUGGY.getGameBinding();
                case COMMS_PANEL -> Bindings.GameCommand.BINDING_FOCUS_COMMS_PANEL_BUGGY.getGameBinding();
                case INTERNAL_PANEL -> Bindings.GameCommand.BINDING_FOCUS_INTERNAL_PANEL_BUGGY.getGameBinding();
                case ROLE_PANEL -> Bindings.GameCommand.BINDING_FOCUS_ROLE_PANEL_BUGGY.getGameBinding();
                default -> throw new IllegalArgumentException("No panel binding for GuiFocus: " + panel);
            };
        } else if (status.isInMainShip() || status.isInFighter()) {
            binding = switch (panel) {
                case EXTERNAL_PANEL -> Bindings.GameCommand.BINDING_FOCUS_LEFT_PANEL.getGameBinding();
                case COMMS_PANEL -> Bindings.GameCommand.BINDING_FOCUS_COMMS_PANEL.getGameBinding();
                case INTERNAL_PANEL -> Bindings.GameCommand.BINDING_FOCUS_INTERNAL_PANEL.getGameBinding();
                case ROLE_PANEL -> Bindings.GameCommand.BINDING_FOCUS_ROLE_PANEL.getGameBinding();
                default -> throw new IllegalArgumentException("No panel binding for GuiFocus: " + panel);
            };
        }
        if (binding != null) {
            GameControllerBus.publish(GameInputSequenceEvent.of(
                    GameInputStep.bindingTap(binding), // panel focus keys must be tapped, not held
                    GameInputStep.delay(RANDOM_MAX)
            ));
        }
    }

    private void closePanel(GuiFocus panel) {
        // Pressing the panel's own open key again closes it
        openPanel(panel);
    }

    public static int randomDelay() {
        return RANDOM_MIN + ThreadLocalRandom.current().nextInt(RANDOM_MAX - RANDOM_MIN + 1);
    }

    private void inputDelay(int delayMs) {
        GameControllerBus.publish(GameInputSequenceEvent.single(GameInputStep.delay(delayMs)));
    }
}
