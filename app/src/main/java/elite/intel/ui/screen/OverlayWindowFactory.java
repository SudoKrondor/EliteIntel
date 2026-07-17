package elite.intel.ui.screen;

import elite.intel.ui.overlay.CompanionOverlayWindow;
import elite.intel.ui.overlay.OverlayWindow;

import java.util.Locale;
import java.util.Objects;

/** Selects the overlay presentation appropriate for the active desktop platform. */
final class OverlayWindowFactory {

    private OverlayWindowFactory() {
    }

    /** Creates the legacy conversation window on Linux and the current companion overlay everywhere else. */
    static OverlayWindow create(Runnable onHidden) {
        Objects.requireNonNull(onHidden, "onHidden");
        if (usesLegacyLinuxOverlay(System.getProperty("os.name", ""))) {
            return new OBSOverlayWindow(onHidden);
        }
        return new CompanionOverlayWindow(onHidden);
    }

    /** Returns whether the supplied operating-system name must use the legacy Linux presentation. */
    static boolean usesLegacyLinuxOverlay(String operatingSystemName) {
        return operatingSystemName != null
                && operatingSystemName.toLowerCase(Locale.ROOT).contains("linux");
    }
}
