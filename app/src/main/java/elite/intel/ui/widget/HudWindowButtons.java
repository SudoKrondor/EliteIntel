package elite.intel.ui.widget;

import com.formdev.flatlaf.util.SystemInfo;
import elite.intel.ui.theme.HudGlyphs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowStateListener;
import java.util.Objects;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static elite.intel.ui.theme.HudPalette.*;

/**
 * The main window's own minimize, maximize/restore and close buttons, at the right end of the app header
 * ({@link TopStatusBar}). They stand in for the operating system's title-bar buttons, which the main window no
 * longer shows (ED_HUD_REFERENCE §11.1). Glyph only, like the dialog close button: muted at rest, the primary action
 * colour on hover, and danger on hover for close.
 * <p>
 * The maximize button follows the window: it shows the restore glyph while the window is maximized, whoever
 * maximized it (this button, a double-click on the header, Windows Snap). Call {@link #dispose()} when discarding.
 */
public final class HudWindowButtons extends JPanel {

    private final Frame frame;
    private final HudGlyphButton maximizeButton;
    private final WindowStateListener stateListener;

    /**
     * @param frame   the window the buttons act on
     * @param onClose the window's own close path, run by the close button - the same one the window's close request
     *                runs, so nothing is skipped
     */
    public HudWindowButtons(Frame frame, Runnable onClose) {
        this.frame = Objects.requireNonNull(frame, "frame");
        Objects.requireNonNull(onClose, "onClose");
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        HudGlyphButton minimize = new HudGlyphButton(HudGlyphs::paintHudMinimizeGlyph,
                HUD_COLOR_ROLE_CONTROL_DECORATION, HUD_COLOR_ROLE_PRIMARY_ACTION, getText("window.minimize"),
                () -> frame.setExtendedState(minimized(frame.getExtendedState())));
        maximizeButton = new HudGlyphButton(HudGlyphs::paintHudMaximizeGlyph,
                HUD_COLOR_ROLE_CONTROL_DECORATION, HUD_COLOR_ROLE_PRIMARY_ACTION, getText("window.maximize"),
                () -> frame.setExtendedState(maximizeToggled(frame.getExtendedState(), SystemInfo.isLinux)));
        HudGlyphButton close = new HudGlyphButton(HudGlyphs::paintHudCloseGlyph,
                HUD_COLOR_ROLE_CONTROL_DECORATION, HUD_COLOR_ROLE_DANGER, getText("window.close"), onClose);

        for (HudGlyphButton b : new HudGlyphButton[]{minimize, maximizeButton, close}) {
            b.setAlignmentY(Component.CENTER_ALIGNMENT);
            b.setMaximumSize(b.getPreferredSize());
        }
        add(minimize);
        add(Box.createHorizontalStrut(HUD_GAP));
        add(maximizeButton);
        add(Box.createHorizontalStrut(HUD_GAP));
        add(close);

        showState(frame.getExtendedState());
        stateListener = e -> showState(e.getNewState());
        frame.addWindowStateListener(stateListener);
    }

    /** Stops following the window. Must be called when this component is discarded. */
    public void dispose() {
        frame.removeWindowStateListener(stateListener);
    }

    private void showState(int extendedState) {
        if (isMaximized(extendedState)) {
            maximizeButton.setGlyph(HudGlyphs::paintHudRestoreGlyph, getText("window.restore"));
        } else {
            maximizeButton.setGlyph(HudGlyphs::paintHudMaximizeGlyph, getText("window.maximize"));
        }
    }

    /** True when the window fills the screen both ways - the state the restore glyph stands for. */
    static boolean isMaximized(int extendedState) {
        return (extendedState & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
    }

    /**
     * The state the maximize button asks for: restored when maximized, maximized otherwise. Any other bits are kept.
     * <p>
     * WHY the Linux case: Java's X11 code XORs the requested state with the current one, so asking a window that is
     * maximized vertically only (dragged to a screen edge) for {@code MAXIMIZED_BOTH} leaves it maximized
     * horizontally only. Asking for {@code MAXIMIZED_HORIZ} there ends in {@code MAXIMIZED_BOTH}. This is FlatLaf's
     * own workaround in its title-bar maximize button, which ours replaces (review, 2026-11-14).
     *
     * @param linux whether the window runs on Linux ({@code SystemInfo.isLinux})
     */
    static int maximizeToggled(int extendedState, boolean linux) {
        if (isMaximized(extendedState)) {
            return extendedState & ~Frame.MAXIMIZED_BOTH;
        }
        if (linux && (extendedState & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_VERT) {
            return (extendedState & ~Frame.MAXIMIZED_BOTH) | Frame.MAXIMIZED_HORIZ;
        }
        return extendedState | Frame.MAXIMIZED_BOTH;
    }

    /**
     * The state the minimize button asks for. WHY the maximized bits are kept: a window minimized while maximized
     * comes back maximized from the taskbar, as with the system's own button.
     */
    static int minimized(int extendedState) {
        return extendedState | Frame.ICONIFIED;
    }
}
