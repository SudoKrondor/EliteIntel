package elite.intel.session.ui;

import elite.intel.session.StatusFlags.GuiFocus;

/**
 * Tracks which tab the player is on for each of the four ship UI panels.
 * <p>
 * This class is pure state - no keyboard, no command handlers, no Bindings.
 * It lives in PlayerSession and is safe to access from anywhere including
 * StatusEventSubscriber.
 * <p>
 * UINavigator (in the command handler layer) reads and mutates this state
 * when it dispatches keystrokes, keeping the two concerns fully separated.
 */
public class PanelStateTracker {

    private static volatile PanelStateTracker instance;

    private final PanelState<LeftPanel> leftPanel = new PanelState<>(LeftPanel.NAVIGATION);
    private final PanelState<CommsPanel> commsPanel = new PanelState<>(CommsPanel.CHAT);
    private final PanelState<CenterPanel> centrePanel = new PanelState<>(CenterPanel.COMMANDER);
    private final PanelState<RightPanel> rightPanel = new PanelState<>(RightPanel.MAIN);

    // True while EliteIntel is the one who opened the current panel.
    // Set by UINavigator before dispatching the open keystroke.
    // Cleared here when GuiFocus returns to NO_FOCUS.
    private volatile boolean eliteIntelOpenedPanel = false;

    // The panel EliteIntel most recently opened. Used by ClosePanelHandler
    // so a generic "close" command knows which panel to restore and close
    // without the user having to specify it.
    private volatile GuiFocus lastOpenedPanel = null;

    private PanelStateTracker() {
    }

    public static PanelStateTracker getInstance() {
        if (instance == null) {
            synchronized (PanelStateTracker.class) {
                if (instance == null) {
                    instance = new PanelStateTracker();
                }
            }
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // Called by StatusEventSubscriber on every GuiFocus transition
    // -------------------------------------------------------------------------

    /**
     * Must be called whenever GuiFocus changes in the status event loop.
     * If the player opened the panel (not EliteIntel), marks that panel's
     * tab position as unknown since we can't observe where they navigated.
     */
    public void onGuiFocusChanged(GuiFocus newFocus) {
        if (newFocus == GuiFocus.NO_FOCUS) {
            //eliteIntelOpenedPanel = false;
            return;
        }
        if (!eliteIntelOpenedPanel) {
            // Player opened the panel - tab position is now unknown
            PanelState<?> state = getState(newFocus);
            if (state == null) {
                return;
            }
            state.markUnknown();
        }
    }

    // -------------------------------------------------------------------------
    // Called by UINavigator before/after dispatching keystrokes
    // -------------------------------------------------------------------------

    /**
     * Called by UINavigator just before it sends the open-panel keystroke.
     */
    public void notifyEliteIntelOpeningPanel(GuiFocus panel) {
        eliteIntelOpenedPanel = true;
        lastOpenedPanel = panel;
    }

    /**
     * Called by UINavigator just after it sends the close-panel keystroke.
     */
    public void notifyEliteIntelClosedPanel() {
        eliteIntelOpenedPanel = false;
        lastOpenedPanel = null;
    }

    // -------------------------------------------------------------------------
    // Panel state accessors - used by UINavigator for delta navigation
    // -------------------------------------------------------------------------

    public PanelState<LeftPanel> getLeftPanel() {
        return leftPanel;
    }

    public PanelState<CommsPanel> getCommsPanel() {
        return commsPanel;
    }

    public PanelState<CenterPanel> getCentrePanel() {
        return centrePanel;
    }

    public PanelState<RightPanel> getRightPanel() {
        return rightPanel;
    }

    /**
     * Returns the panel EliteIntel last opened, or null if no panel is currently open.
     */
    public GuiFocus getLastOpenedPanel() {
        return lastOpenedPanel;
    }

    public boolean isEliteIntelOpenedPanel() {
        return eliteIntelOpenedPanel;
    }

    public PanelState<?> getState(GuiFocus panel) {
        if (panel == GuiFocus.EXTERNAL_PANEL) {
            return leftPanel;
        }
        if (panel == GuiFocus.COMMS_PANEL) {
            return commsPanel;
        }
        if (panel == GuiFocus.CENTRAL_PANEL) {
            return centrePanel;
        }
        if (panel == GuiFocus.INTERNAL_PANEL) {
            return rightPanel;
        }
        return null;
    }
}