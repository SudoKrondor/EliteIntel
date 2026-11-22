package elite.intel.ui.widget;

import org.junit.jupiter.api.Test;

import java.awt.Frame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The main window's own buttons stand in for the system title bar's (2026-11-14), so the state each one asks for has
 * to be the one the system's button would: maximize and restore flip, and minimize keeps a maximized window maximized
 * for when it comes back from the taskbar.
 */
class HudWindowButtonsTest {

    @Test
    void maximizeFlipsBetweenNormalAndMaximized() {
        for (boolean linux : new boolean[]{false, true}) {
            assertEquals(Frame.MAXIMIZED_BOTH, HudWindowButtons.maximizeToggled(Frame.NORMAL, linux));
            assertEquals(Frame.NORMAL, HudWindowButtons.maximizeToggled(Frame.MAXIMIZED_BOTH, linux));
        }
    }

    @Test
    void aWindowMaximizedOneWayOnlyIsMaximizedFully() {
        // A half-snapped window is not maximized: the button maximizes it rather than restoring it.
        assertFalse(HudWindowButtons.isMaximized(Frame.MAXIMIZED_VERT));
        assertEquals(Frame.MAXIMIZED_BOTH, HudWindowButtons.maximizeToggled(Frame.MAXIMIZED_VERT, false));
        assertEquals(Frame.MAXIMIZED_BOTH, HudWindowButtons.maximizeToggled(Frame.MAXIMIZED_HORIZ, true));
    }

    @Test
    void onLinuxAVerticallyMaximizedWindowAsksForHorizontal() {
        // X11 XORs the request with the current state: VERT ^ HORIZ is BOTH. FlatLaf's own workaround (review, 2026-11-14).
        assertEquals(Frame.MAXIMIZED_HORIZ, HudWindowButtons.maximizeToggled(Frame.MAXIMIZED_VERT, true));
    }

    @Test
    void minimizeKeepsTheMaximizedState() {
        int state = HudWindowButtons.minimized(Frame.MAXIMIZED_BOTH);

        assertEquals(Frame.ICONIFIED, state & Frame.ICONIFIED);
        assertTrue(HudWindowButtons.isMaximized(state), "comes back maximized from the taskbar");
    }

    @Test
    void minimizeFromNormalIsJustIconified() {
        assertEquals(Frame.ICONIFIED, HudWindowButtons.minimized(Frame.NORMAL));
    }
}
