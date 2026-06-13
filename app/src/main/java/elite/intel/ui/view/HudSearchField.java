package elite.intel.ui.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Reusable HUD search input with placeholder text and an embedded clear action.
 * <p>
 * {@link Variant#TABLE_FILTER} renders as a segmented Elite-Dangerous-style filter bar:
 * a dedicated icon cell, a text cell, and a clear cell, separated by {@code HUD_BORDER_DIM}
 * vertical lines, with {@code HUD_CYAN_SOFT} L-shaped corner marks painted over the outer border.
 * <p>
 * {@link Variant#TABLE_FILTER_CONNECTED} is the same segmented layout but without a bottom
 * border line — it instead draws a dim separator. Use it with a data table directly below so
 * that the shared side borders create one continuous framed block.
 * <p>
 * {@link Variant#EMBEDDED} uses the same segmented layout as {@code TABLE_FILTER} but paints
 * no outer border at all. Use it inside a {@link HudConnectedToolbar} or any other container
 * that already provides the outer frame.
 */
public class HudSearchField extends JPanel {

    static final String HUD_SEARCH_INNER_FIELD = "eliteIntel.hud.searchInnerField";
    static final String HUD_SEARCH_CLEAR_BUTTON = "eliteIntel.hud.searchClearButton";

    private static final int CORNER_MARK = 6;

    /** Visual treatment for reusable HUD search controls. */
    public enum Variant {
        /** Compact standalone search input for toolbars. */
        STANDARD,
        /** Full-width table filter bar with a fully enclosed border and corner marks. */
        TABLE_FILTER,
        /**
         * Full-width table filter bar without a bottom border line.
         * Draws a dim bottom separator instead and top-only corner marks so that a data table
         * placed directly below shares the same left/right/bottom border, forming one unified block.
         */
        TABLE_FILTER_CONNECTED,
        /**
         * Borderless segmented filter layout (icon | text | clear) for embedding inside a
         * {@link HudConnectedToolbar}. No outer border or corner marks are painted; the host
         * toolbar provides the frame.
         */
        EMBEDDED
    }

    private final JTextField textField;
    private final Variant variant;

    /**
     * Creates a search field wrapper that owns the HUD border and clear button.
     *
     * @param placeholder  localized placeholder and tooltip text
     * @param clearTooltip localized tooltip for the clear button
     */
    public HudSearchField(String placeholder, String clearTooltip) {
        this(placeholder, clearTooltip, Variant.STANDARD);
    }

    /**
     * Creates a search field wrapper with an explicit HUD visual treatment.
     *
     * @param placeholder  localized placeholder and tooltip text
     * @param clearTooltip localized tooltip for the clear button
     * @param variant      visual treatment for this search control
     */
    public HudSearchField(String placeholder, String clearTooltip, Variant variant) {
        super(new BorderLayout());
        this.variant = variant == null ? Variant.STANDARD : variant;
        boolean filter = this.variant == Variant.TABLE_FILTER
                || this.variant == Variant.TABLE_FILTER_CONNECTED
                || this.variant == Variant.EMBEDDED;

        setOpaque(true);
        setBackground(filter ? AppTheme.HUD_PANEL_BG : AppTheme.HUD_PANEL_BG_ALT);
        // TABLE_FILTER variants paint their own border in paintBorder(); use a 1 px inset so
        // child panels don't bleed over the painted border line.
        // EMBEDDED has no outer border — the host container provides the frame.
        if (this.variant == Variant.EMBEDDED) {
            setBorder(new EmptyBorder(0, 0, 0, 0));
        } else {
            setBorder(filter ? new EmptyBorder(1, 1, 1, 1) : AppTheme.hudFieldBorder());
        }

        textField = new PlaceholderTextField(placeholder);
        textField.putClientProperty(HUD_SEARCH_INNER_FIELD, Boolean.TRUE);
        textField.setOpaque(false);
        textField.setBorder(BorderFactory.createEmptyBorder(0, filter ? 10 : 0, 0, 6));
        AppTheme.styleTextComponent(textField);
        textField.setOpaque(false);

        JButton clearButton = new JButton("×");
        clearButton.putClientProperty(HUD_SEARCH_CLEAR_BUTTON, Boolean.TRUE);
        clearButton.putClientProperty("eliteIntel.hud.lockedForeground", Boolean.TRUE);
        clearButton.setToolTipText(clearTooltip);
        clearButton.setOpaque(false);
        clearButton.setContentAreaFilled(false);
        clearButton.setBorderPainted(false);
        clearButton.setFocusable(false);
        clearButton.setForeground(AppTheme.FG_MUTED);
        clearButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearButton.addActionListener(e -> textField.setText(""));

        if (filter) {
            add(iconSegment(), BorderLayout.WEST);
            add(textField, BorderLayout.CENTER);
            add(clearSegment(clearButton), BorderLayout.EAST);
        } else {
            JLabel iconLabel = new JLabel(new SearchIcon());
            iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 9));
            clearButton.setMargin(new Insets(0, 8, 0, 8));
            add(iconLabel, BorderLayout.WEST);
            add(textField, BorderLayout.CENTER);
            add(clearButton, BorderLayout.EAST);
        }
    }

    /** Returns the underlying text field so callers can attach document listeners. */
    public JTextField textField() {
        return textField;
    }

    // -- Segment builders -------------------------------------------------------

    private static JPanel iconSegment() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(true);
        panel.setBackground(AppTheme.HUD_BG);
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, AppTheme.HUD_BORDER_DIM));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 10, 0, 10);
        panel.add(new JLabel(new SearchIcon()), gbc);
        return panel;
    }

    private static JPanel clearSegment(JButton clearButton) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(true);
        panel.setBackground(AppTheme.HUD_BG);
        panel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, AppTheme.HUD_BORDER_DIM));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 8, 0, 8);
        panel.add(clearButton, gbc);
        return panel;
    }

    // -- Custom border for TABLE_FILTER -----------------------------------------

    @Override
    protected void paintBorder(Graphics g) {
        if (variant != Variant.TABLE_FILTER && variant != Variant.TABLE_FILTER_CONNECTED) {
            super.paintBorder(g);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int m = CORNER_MARK;
            if (variant == Variant.TABLE_FILTER) {
                // Full enclosing border
                g2.setColor(AppTheme.HUD_BORDER);
                g2.drawRect(0, 0, w - 1, h - 1);
                // Corner accent marks at all four corners
                g2.setColor(AppTheme.HUD_CYAN_SOFT);
                g2.drawLine(1, 1, 1 + m, 1);       g2.drawLine(1, 1, 1, 1 + m);           // TL
                g2.drawLine(w-2-m, 1, w-2, 1);     g2.drawLine(w-2, 1, w-2, 1 + m);       // TR
                g2.drawLine(1, h-2, 1+m, h-2);     g2.drawLine(1, h-2-m, 1, h-2);         // BL
                g2.drawLine(w-2-m, h-2, w-2, h-2); g2.drawLine(w-2, h-2-m, w-2, h-2);    // BR
            } else {
                // TABLE_FILTER_CONNECTED: top/left/right border only — the table provides the bottom
                g2.setColor(AppTheme.HUD_BORDER);
                g2.drawLine(0, 0, w - 1, 0);           // top
                g2.drawLine(0, 0, 0, h - 1);           // left
                g2.drawLine(w - 1, 0, w - 1, h - 1);  // right
                // Dim separator line at the bottom (marks the filter/table boundary)
                g2.setColor(AppTheme.HUD_BORDER_DIM);
                g2.drawLine(0, h - 1, w - 1, h - 1);
                // Corner accent marks on top corners only
                g2.setColor(AppTheme.HUD_CYAN_SOFT);
                g2.drawLine(1, 1, 1 + m, 1);      g2.drawLine(1, 1, 1, 1 + m);      // TL
                g2.drawLine(w-2-m, 1, w-2, 1);    g2.drawLine(w-2, 1, w-2, 1 + m);  // TR
            }
        } finally {
            g2.dispose();
        }
    }

    // -- Inner components -------------------------------------------------------

    private static final class PlaceholderTextField extends JTextField {
        private final String placeholder;

        private PlaceholderTextField(String placeholder) {
            this.placeholder = placeholder;
            setToolTipText(placeholder);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (!getText().isEmpty() || placeholder == null || placeholder.isBlank()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setColor(AppTheme.FG_MUTED);
                FontMetrics metrics = g2.getFontMetrics();
                Insets insets = getInsets();
                int x = insets.left + 2;
                int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
                g2.drawString(placeholder, x, y);
            } finally {
                g2.dispose();
            }
        }
    }

    private static final class SearchIcon implements Icon {
        private static final int SIZE = 14;

        @Override
        public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.HUD_CYAN_SOFT);
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawOval(x + 1, y + 1, 8, 8);
                g2.drawLine(x + 9, y + 9, x + 13, y + 13);
            } finally {
                g2.dispose();
            }
        }

        @Override public int getIconWidth()  { return SIZE; }
        @Override public int getIconHeight() { return SIZE; }
    }
}
