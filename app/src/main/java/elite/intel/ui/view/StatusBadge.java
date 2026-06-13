package elite.intel.ui.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Compact status pill for HUD strips and future telemetry cards.
 */
public class StatusBadge extends JLabel {

    public enum State {
        OK(AppTheme.HUD_OK),
        STANDBY(AppTheme.HUD_WARN),
        OFFLINE(AppTheme.HUD_DANGER),
        INFO(AppTheme.HUD_CYAN),
        /** Muted/inactive: service is off or sleeping — no action required. */
        IDLE(AppTheme.HUD_DISABLED);

        private final Color color;

        State(Color color) {
            this.color = color;
        }
    }

    private State state;

    /**
     * Creates a badge with a short label and state colour.
     *
     * @param label visible badge text
     * @param state visual state used for border and text colour
     */
    public StatusBadge(String label, State state) {
        super(label == null ? "" : label.toUpperCase(), SwingConstants.CENTER);
        this.state = state == null ? State.INFO : state;
        setOpaque(false);
        setForeground(this.state.color);
        setFont(getFont().deriveFont(Font.BOLD, AppTheme.HUD_FONT_BADGE_ROLE));
        setBorder(new EmptyBorder(2, 10, 2, 10));
        setMinimumSize(new Dimension(68, AppTheme.HUD_BADGE_HEIGHT));
        updatePreferredSize();
    }

    /**
     * Updates badge text and state on the Swing EDT.
     *
     * @param label visible badge text
     * @param state visual state used for border and text colour
     */
    public void setStatus(String label, State state) {
        this.state = state == null ? State.INFO : state;
        setText(label == null ? "" : label.toUpperCase());
        setForeground(this.state.color);
        updatePreferredSize();
        revalidate();
        repaint();
    }

    private void updatePreferredSize() {
        Insets insets = getInsets();
        int textWidth = getFontMetrics(getFont()).stringWidth(getText());
        int width = Math.max(76, textWidth + insets.left + insets.right + 8);
        setPreferredSize(new Dimension(width, AppTheme.HUD_BADGE_HEIGHT));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            Color fill = new Color(state.color.getRed(), state.color.getGreen(), state.color.getBlue(), 24);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, Math.max(0, w - 1), Math.max(0, h - 1), 10, 10);
            g2.setColor(state.color);
            g2.drawRoundRect(0, 0, Math.max(0, w - 1), Math.max(0, h - 1), 10, 10);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
