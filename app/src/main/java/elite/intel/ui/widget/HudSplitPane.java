package elite.intel.ui.widget;

import elite.intel.ui.theme.AppTheme;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;

/**
 * HUD-styled JSplitPane. Divider background is painted using {@link AppTheme#HUD_BG} so it
 * blends with the main window shell. Grip dots use {@link AppTheme#HUD_DISABLED} — muted but
 * functional. Supports {@link JSplitPane#HORIZONTAL_SPLIT} and {@link JSplitPane#VERTICAL_SPLIT}.
 */
public class HudSplitPane extends JSplitPane {

    /**
     * Creates a HudSplitPane with the given orientation and child components.
     *
     * @param orientation {@link JSplitPane#HORIZONTAL_SPLIT} or {@link JSplitPane#VERTICAL_SPLIT}
     * @param left        left (or top) component
     * @param right       right (or bottom) component
     */
    public HudSplitPane(int orientation, Component left, Component right) {
        super(orientation, left, right);
        applyHudDefaults();
    }

    /**
     * Creates a HudSplitPane with the given orientation without pre-set child components.
     *
     * @param orientation {@link JSplitPane#HORIZONTAL_SPLIT} or {@link JSplitPane#VERTICAL_SPLIT}
     */
    public HudSplitPane(int orientation) {
        super(orientation);
        applyHudDefaults();
    }

    /**
     * Called after the super constructor to set HUD-consistent defaults.
     * {@link #updateUI()} (called by JComponent) already installed our UI, so we only
     * need properties that BasicSplitPaneUI.installDefaults() would otherwise reset.
     */
    private void applyHudDefaults() {
        setBackground(AppTheme.HUD_BG);
        setBorder(null);
        setDividerSize(AppTheme.HUD_GAP);
    }

    /** Reinstalls our custom UI on every LAF change, preventing Swing defaults from overriding. */
    @Override
    public void updateUI() {
        setUI(new HudSplitPaneUI());
    }

    // -- UI -----------------------------------------------------------------------

    private static final class HudSplitPaneUI extends BasicSplitPaneUI {
        @Override
        public BasicSplitPaneDivider createDefaultDivider() {
            return new HudDivider(this);
        }
    }

    // -- Divider ------------------------------------------------------------------

    private static final class HudDivider extends BasicSplitPaneDivider {

        private static final int DOT_SIZE  = 3;
        private static final int DOT_COUNT = 5;
        private static final int DOT_GAP   = 3;

        HudDivider(BasicSplitPaneUI ui) {
            super(ui);
            setBackground(AppTheme.HUD_BG);
            // Remove the default raised-bevel border so the divider has no bright edge.
            setBorder(null);
        }

        /** Fully overrides default paint to avoid any Swing-default background or border artifacts. */
        @Override
        public void paint(Graphics g) {
            g.setColor(AppTheme.HUD_BG);
            g.fillRect(0, 0, getWidth(), getHeight());
            paintGripDots((Graphics2D) g.create());
        }

        private void paintGripDots(Graphics2D g2) {
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.HUD_DISABLED);

                int w = getWidth();
                int h = getHeight();
                int total = DOT_COUNT * DOT_SIZE + (DOT_COUNT - 1) * DOT_GAP;

                boolean horizontalSplit = splitPane != null
                        && splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT;

                if (horizontalSplit) {
                    // Vertical bar divider: dots stacked vertically along the center axis.
                    int x = (w - DOT_SIZE) / 2;
                    int y = (h - total) / 2;
                    for (int i = 0; i < DOT_COUNT; i++) {
                        g2.fillOval(x, y + i * (DOT_SIZE + DOT_GAP), DOT_SIZE, DOT_SIZE);
                    }
                } else {
                    // Horizontal bar divider: dots arranged horizontally along the center axis.
                    int x = (w - total) / 2;
                    int y = (h - DOT_SIZE) / 2;
                    for (int i = 0; i < DOT_COUNT; i++) {
                        g2.fillOval(x + i * (DOT_SIZE + DOT_GAP), y, DOT_SIZE, DOT_SIZE);
                    }
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
