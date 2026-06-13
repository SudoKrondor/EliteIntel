package elite.intel.ui.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import static elite.intel.ui.view.AppTheme.*;

/**
 * HUD-style status readout row: fixed muted label on the left, value right-aligned
 * and coloured by state, thin vertical accent mark on the far left.
 * <p>
 * Designed for use inside a {@code BoxLayout.Y_AXIS} container (e.g. QUICK STATUS panel).
 * All updates must be called on the EDT via {@link #setValue(String, StatusBadge.State)}.
 */
public class HudStatusReadout extends JPanel {

    private Color currentStateColor;
    private final JLabel valueComp;

    /**
     * @param label fixed left-side key label (e.g. "STT"); converted to upper-case
     * @param value initial right-side value text; converted to upper-case
     * @param state initial visual state driving text and accent colour
     */
    public HudStatusReadout(String label, String value, StatusBadge.State state) {
        setLayout(new BorderLayout());
        setOpaque(false);
        setAlignmentX(LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, HUD_BADGE_HEIGHT));
        // 8px left padding reserves space for the 2px accent mark
        setBorder(new EmptyBorder(0, 8, 0, 2));

        currentStateColor = stateColor(state);

        JLabel labelComp = new JLabel(label == null ? "" : label.toUpperCase());
        labelComp.setForeground(FG_MUTED);
        labelComp.setFont(labelComp.getFont().deriveFont(Font.PLAIN, HUD_FONT_READOUT_KEY));

        valueComp = new JLabel(value == null ? "" : value.toUpperCase(), SwingConstants.RIGHT);
        valueComp.setForeground(currentStateColor);
        valueComp.setFont(valueComp.getFont().deriveFont(Font.BOLD, HUD_FONT_READOUT_VALUE));
        // Prevent applyDarkPalette from overriding the state-driven foreground colour.
        valueComp.putClientProperty(AppTheme.HUD_LOCKED_FOREGROUND, Boolean.TRUE);

        add(labelComp, BorderLayout.WEST);
        add(valueComp, BorderLayout.CENTER);
    }

    /**
     * Updates the value text and state colour. Must be called on the EDT.
     *
     * @param value new value text; converted to upper-case, {@code null} becomes empty string
     * @param state new visual state
     */
    public void setValue(String value, StatusBadge.State state) {
        currentStateColor = stateColor(state);
        valueComp.setText(value == null ? "" : value.toUpperCase());
        valueComp.setForeground(currentStateColor);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int h = getHeight();
            g2.setColor(currentStateColor);
            g2.fillRect(0, (h - 12) / 2, 2, 12);
        } finally {
            g2.dispose();
        }
    }

    private static Color stateColor(StatusBadge.State state) {
        if (state == null) return HUD_CYAN;
        return switch (state) {
            case OK      -> HUD_OK;
            case STANDBY -> HUD_WARN;
            case OFFLINE -> HUD_DANGER;
            case INFO    -> HUD_CYAN;
            case IDLE    -> HUD_DISABLED;
        };
    }
}
