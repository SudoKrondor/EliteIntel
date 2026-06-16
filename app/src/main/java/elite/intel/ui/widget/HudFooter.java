package elite.intel.ui.widget;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;

import javax.swing.AbstractButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.List;

/**
 * Shared footer strip for both modal dialogs and non-modal screen/tab panels (HUD section 10).
 * The layout and the warm {@link AppTheme#hudFooterBorder()} rule are identical for both; the
 * only difference is the left slot:
 * <ul>
 *   <li>modal: a BACK/dismiss button;</li>
 *   <li>non-modal: a status/info component - BACK is NOT allowed.</li>
 * </ul>
 * The {@code modal} flag selects which left component is shown, structurally preventing a BACK
 * button from ever appearing in a non-modal footer.
 */
public final class HudFooter {

    private HudFooter() {}

    /**
     * Builds a footer panel: warm footer rule on top, a left slot, and right-aligned actions.
     *
     * @param modal    true for a modal dialog footer (shows {@code back} on the left);
     *                 false for a non-modal screen/tab footer (shows {@code status} on the left, no BACK)
     * @param back     the BACK/dismiss button - used only when {@code modal} is true; may be null
     * @param status   a status/info component for the left slot - used only when {@code modal} is
     *                 false; may be null
     * @param trailing right-aligned action buttons in visual left-to-right order (primary last); may be null
     */
    public static JPanel build(boolean modal, AbstractButton back, Component status,
                               List<? extends AbstractButton> trailing) {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(AppTheme.hudFooterBorder());

        JPanel west = new JPanel(new FlowLayout(FlowLayout.LEFT, HudPalette.HUD_GAP, 0));
        west.setOpaque(false);
        Component leading = modal ? back : status;
        if (leading != null) {
            west.add(leading);
        }

        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, HudPalette.HUD_GAP, 0));
        east.setOpaque(false);
        if (trailing != null) {
            for (AbstractButton button : trailing) {
                east.add(button);
            }
        }

        footer.add(west, BorderLayout.WEST);
        footer.add(east, BorderLayout.EAST);
        return footer;
    }
}
