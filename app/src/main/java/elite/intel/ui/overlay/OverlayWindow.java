package elite.intel.ui.overlay;

/**
 * Platform-specific presentation surface for the companion overlay. Implementations own their event subscriptions
 * while visible and must release them when hidden or disposed.
 */
public interface OverlayWindow {

    /** Shows the overlay and begins receiving its live data. */
    void showOverlay();

    /** Hides the overlay and stops receiving its live data. */
    void hideOverlay();

    /** Permanently releases the native window and all live subscriptions. */
    void dispose();
}
