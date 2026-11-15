package elite.intel.ui.inputmonitor;

import elite.intel.ui.theme.HudPalette;

import java.awt.*;

/**
 * The Input Monitor tab's own names for the colours it uses.
 * <p>
 * Each one names a {@link HudPalette} role, never a literal colour: the tab is part of the app window, so it
 * must follow the commander's colour theme like every other screen. The readout windows in {@code overlay/}
 * are different: they float over the game, and their colours are the commander's settings in each readout's
 * own dialog, so they are not drawn from here.
 */
public final class InputMonitorPalette {

    private InputMonitorPalette() {
    }

    public static final Color DESCRIPTION_TEXT = HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT;
    public static final Color STATUS_PENDING_TEXT = HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT;
    public static final Color STATUS_UNAVAILABLE_TEXT = HudPalette.HUD_COLOR_ROLE_DANGER;
}
