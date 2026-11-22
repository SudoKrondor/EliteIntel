package elite.intel.ui.theme;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;

/**
 * The colour theme this run of the app draws with.
 * <p>
 * {@link HudPalette}'s colour roles are {@code static final}: they are read from here once, when the class first
 * loads, and keep those values until the app exits. That is why a theme is installed once at startup, before
 * anything touches the palette, and why choosing another theme takes effect on the next start. A live switch would
 * mean re-colouring every component the app has already built.
 * <p>
 * A theme installed too late cannot take effect, so both ways of getting the order wrong log an error rather than
 * leaving a commander with a theme that silently did not apply.
 */
public final class ActiveTheme {

    private static final Logger log = LogManager.getLogger(ActiveTheme.class);

    private static volatile ColorTheme theme;
    private static volatile boolean paletteRead;

    private ActiveTheme() {
    }

    /** Sets the theme for this run. Call it once, before anything builds a window or touches {@link HudPalette}. */
    public static synchronized void install(ColorTheme chosen) {
        if (paletteRead) {
            log.error("Colour theme '{}' installed after the palette was read; it takes effect on the next start",
                    chosen.name());
            return;
        }
        theme = chosen;
    }

    /** The colour the installed theme gives {@code role}, for {@link HudPalette}'s initialisation. */
    static synchronized Color color(ThemeRole role) {
        if (!paletteRead && theme == null) {
            log.error("The palette was read before a colour theme was installed; Default is used for this run");
        }
        paletteRead = true;
        return (theme == null ? ColorTheme.DEFAULT : theme).color(role);
    }
}
