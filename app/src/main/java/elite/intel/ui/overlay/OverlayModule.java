package elite.intel.ui.overlay;

import javax.swing.*;

/**
 * A self-contained surface inside the companion overlay. A module owns its visual component and every external
 * subscription or timer it needs while the overlay is visible.
 */
interface OverlayModule {

    /** Returns the module's stable root component for composition by the overlay window. */
    JComponent component();

    /** Starts live updates after the overlay becomes visible. Calls are idempotent. */
    void start();

    /** Stops live updates before the overlay is hidden. Calls are idempotent. */
    void stop();
}
