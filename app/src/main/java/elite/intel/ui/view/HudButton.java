package elite.intel.ui.view;

import javax.swing.*;
import java.awt.*;

/**
 * HUD-styled Swing button with consistent cockpit fill, border, and hover states.
 */
public class HudButton extends JButton {

    private static final Color PRIMARY_FILL          = new Color(0xB04000);
    private static final Color PRIMARY_FILL_HOVER    = new Color(0xCC4D00);
    private static final Color PRIMARY_FILL_PRESSED  = new Color(0xFF6000);

    private final boolean primary;

    /** When > 0, the button uses a fixed square footprint instead of the default action sizing. */
    private int squareSide = 0;

    /**
     * Creates a reusable HUD button.
     *
     * @param label   visible button text
     * @param primary true for the orange primary treatment, false for a subdued treatment
     */
    public HudButton(String label, boolean primary) {
        super(label != null ? label.toUpperCase() : "");
        this.primary = primary;
        setOpaque(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setForeground(primary ? AppTheme.BUTTON_FG : AppTheme.ACCENT);
        setFont(getFont().deriveFont(Font.BOLD, AppTheme.HUD_FONT_BUTTON));
        setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 14));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // Prevent applyDarkPalette from overriding the state-driven foreground colour.
        putClientProperty(AppTheme.HUD_LOCKED_FOREGROUND, Boolean.TRUE);
        // Invert text to dark on the bright ACCENT pressed fill for secondary buttons.
        if (!primary) {
            getModel().addChangeListener(e -> setForeground(getModel().isPressed() ? AppTheme.SEL_FG : AppTheme.ACCENT));
        }
    }

    /**
     * Switches the button to a fixed {@code side}×{@code side} footprint, used for compact
     * trailing field actions (e.g. pickers) that must align with a field's height and stay
     * narrow. Pass 0 to restore the default action-button sizing.
     */
    public void setSquareSide(int side) {
        this.squareSide = side;
        // Drop the wide text-button insets so the glyph/label centres in the square footprint.
        setBorder(side > 0 ? BorderFactory.createEmptyBorder() : BorderFactory.createEmptyBorder(4, 14, 4, 14));
        revalidate();
    }

    @Override
    public Dimension getPreferredSize() {
        if (squareSide > 0) {
            return new Dimension(squareSide, squareSide);
        }
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(90, d.width), AppTheme.HUD_BUTTON_HEIGHT);
    }

    @Override
    public Dimension getMinimumSize() {
        return squareSide > 0 ? getPreferredSize() : super.getMinimumSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return squareSide > 0 ? getPreferredSize() : super.getMaximumSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            int w = getWidth();
            int h = getHeight();
            ButtonModel model = getModel();

            if (!isEnabled()) {
                // Unified disabled appearance for primary and secondary: warm fill + dim cold border.
                g2.setColor(AppTheme.HUD_TABLE_ROW);
                g2.fillRect(0, 0, w - 1, h - 1);
                g2.setColor(AppTheme.HUD_BORDER_DIM);
                g2.drawRect(0, 0, w - 1, h - 1);
            } else if (primary) {
                paintPrimary(g2, w, h, model);
            } else {
                paintSecondary(g2, w, h, model);
            }
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }

    private void paintPrimary(Graphics2D g2, int w, int h, ButtonModel model) {
        Color fill = model.isPressed() ? PRIMARY_FILL_PRESSED
                   : model.isRollover() ? PRIMARY_FILL_HOVER
                   : PRIMARY_FILL;
        g2.setColor(fill);
        g2.fillRect(0, 0, w - 1, h - 1);
        g2.setColor(AppTheme.ACCENT);
        g2.drawRect(0, 0, w - 1, h - 1);
        Color glow = new Color(AppTheme.ACCENT.getRed(), AppTheme.ACCENT.getGreen(),
                AppTheme.ACCENT.getBlue(), 70);
        g2.setColor(glow);
        g2.drawRect(1, 1, w - 3, h - 3);
    }

    private void paintSecondary(Graphics2D g2, int w, int h, ButtonModel model) {
        Color fill = model.isPressed() ? AppTheme.ACCENT
                   : model.isRollover() ? AppTheme.HUD_TABLE_ROW_HOVER
                   : AppTheme.HUD_TABLE_ROW;
        Color border = AppTheme.HUD_ORANGE_SOFT;
        g2.setColor(fill);
        g2.fillRect(0, 0, w - 1, h - 1);
        g2.setColor(border);
        g2.drawRect(0, 0, w - 1, h - 1);
    }
}
