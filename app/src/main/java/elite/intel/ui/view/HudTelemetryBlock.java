package elite.intel.ui.view;

import javax.swing.*;
import java.awt.*;

/**
 * Compact cockpit block showing a muted uppercase label above a readable value.
 * Intended for use inside {@link HudTelemetryStrip} to build horizontal telemetry readouts.
 * Call {@link #setValue(String)} to update the displayed value; pass {@code null} or blank
 * to revert to the {@link #PLACEHOLDER} dash.
 * <p>
 * Layout: a metric icon (or fallback cyan diamond) on the left, vertically centered
 * against the label/value text stack on the right.
 */
public class HudTelemetryBlock extends JPanel {

    /** Placeholder displayed when data is not yet available. */
    public static final String PLACEHOLDER = "—";

    /** Side length used for metric icons passed to this block. */
    public static final int ICON_SIZE = AppTheme.HUD_ICON_SMALL;

    private final JLabel valueLabel;

    /**
     * Creates a block with the diamond fallback marker.
     *
     * @param label short display label; converted to uppercase automatically
     */
    public HudTelemetryBlock(String label) {
        this(label, null);
    }

    /**
     * Creates a block with a PNG metric icon on the left.
     * If {@code icon} is {@code null} the cyan diamond marker is used as a fallback.
     *
     * @param label short display label; converted to uppercase automatically
     * @param icon  pre-scaled icon, or {@code null} to fall back to the diamond marker
     */
    public HudTelemetryBlock(String label, ImageIcon icon) {
        setLayout(new BorderLayout(6, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JLabel keyLabel = new JLabel(label == null ? "" : label.toUpperCase());
        keyLabel.setForeground(AppTheme.HUD_DISABLED);
        keyLabel.setFont(keyLabel.getFont().deriveFont(Font.PLAIN, AppTheme.HUD_FONT_READOUT_KEY));
        keyLabel.setAlignmentX(LEFT_ALIGNMENT);

        valueLabel = new JLabel(PLACEHOLDER);
        valueLabel.setForeground(AppTheme.HUD_DISABLED);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, AppTheme.HUD_FONT_READOUT_VALUE));
        valueLabel.setAlignmentX(LEFT_ALIGNMENT);

        JPanel textStack = new JPanel();
        textStack.setOpaque(false);
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.add(keyLabel);
        textStack.add(valueLabel);

        add(icon != null ? buildIconMarker(icon) : buildDiamondMarker(), BorderLayout.WEST);
        add(textStack, BorderLayout.CENTER);
    }

    /** Updates the displayed value; {@code null} or blank reverts to {@link #PLACEHOLDER} in dim color. */
    public void setValue(String value) {
        boolean placeholder = (value == null || value.isBlank());
        valueLabel.setText(placeholder ? PLACEHOLDER : value);
        valueLabel.setForeground(placeholder ? AppTheme.HUD_DISABLED : AppTheme.FG);
    }

    /** Wraps a pre-tinted native-size icon in a vertically centered JLabel. */
    private static JComponent buildIconMarker(ImageIcon icon) {
        JLabel label = new JLabel(icon);
        label.setOpaque(false);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
        return label;
    }

    /**
     * Paints a small filled diamond as a shared HUD marker for all telemetry blocks.
     * Height is unconstrained so BorderLayout can stretch it for vertical centering.
     */
    private static JComponent buildDiamondMarker() {
        return new JComponent() {
            {
                setOpaque(false);
                setPreferredSize(new Dimension(16, 16));
                setMinimumSize(new Dimension(16, 16));
                setMaximumSize(new Dimension(16, Integer.MAX_VALUE));
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int cx = getWidth() / 2;
                    int cy = getHeight() / 2;
                    int h = 5;
                    int[] xp = {cx, cx + h, cx, cx - h};
                    int[] yp = {cy - h, cy, cy + h, cy};
                    g2.setColor(AppTheme.FG_MUTED);
                    g2.fillPolygon(xp, yp, 4);
                } finally {
                    g2.dispose();
                }
            }
        };
    }
}
