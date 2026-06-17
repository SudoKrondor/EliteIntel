package elite.intel.ui.widget;
import static elite.intel.ui.theme.HudPalette.*;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Reusable HUD search input with placeholder text and an embedded clear action.
 * <p>
 * {@link Variant#TABLE_FILTER} renders as a segmented Elite-Dangerous-style filter bar:
 * a dedicated icon cell, a text cell, and a clear cell, separated by {@code HUD_COLOR_ROLE_SECONDARY_BORDER}
 * vertical lines, with {@code HUD_COLOR_ROLE_INFORMATION_MARK} L-shaped corner marks painted over the outer border.
 * <p>
 * {@link Variant#TABLE_FILTER_CONNECTED} is the same segmented layout but without a bottom
 * border line - it instead draws a dim separator. Use it with a data table directly below so
 * that the shared side borders create one continuous framed block.
 * <p>
 * {@link Variant#EMBEDDED} uses the same segmented layout as {@code TABLE_FILTER} but paints
 * no outer border at all. Use it inside a {@link HudConnectedToolbar} or any other container
 * that already provides the outer frame.
 */
public class HudSearchField extends JPanel {

    public static final String HUD_SEARCH_INNER_FIELD = "eliteIntel.hud.searchInnerField";
    public static final String HUD_SEARCH_CLEAR_BUTTON = "eliteIntel.hud.searchClearButton";

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
     * Whether the inner text field currently holds keyboard focus. Drives the cyan focus accent
     * on the framed variants ({@link Variant#TABLE_FILTER}, {@link Variant#TABLE_FILTER_CONNECTED}):
     * the frame line brightens from {@code HUD_COLOR_ROLE_FRAME_BORDER} (teal) to
     * {@code HUD_COLOR_ROLE_INFORMATION} (cyan) so the focus cue stays in the data/info colour
     * language and does not compete with the orange action buttons that sit beside the field.
     */
    private boolean focused;

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
        setBackground(filter ? HudPalette.HUD_COLOR_ROLE_PANEL_BACKGROUND : HudPalette.HUD_COLOR_ROLE_SECONDARY_PANEL_BACKGROUND);
        // TABLE_FILTER variants paint their own border in paintBorder(); use a 1 px inset so
        // child panels don't bleed over the painted border line.
        // EMBEDDED has no outer border - the host container provides the frame.
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
        // Focus accent: repaint the framed border in cyan while the field is being edited.
        textField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { focused = true; repaint(); }
            @Override public void focusLost(FocusEvent e) { focused = false; repaint(); }
        });

        JButton clearButton = new JButton("×");
        clearButton.putClientProperty(HUD_SEARCH_CLEAR_BUTTON, Boolean.TRUE);
        clearButton.putClientProperty("eliteIntel.hud.lockedForeground", Boolean.TRUE);
        clearButton.setToolTipText(clearTooltip);
        clearButton.setOpaque(false);
        clearButton.setContentAreaFilled(false);
        clearButton.setBorderPainted(false);
        clearButton.setFocusable(false);
        clearButton.setForeground(HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
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
        panel.setBackground(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, HudPalette.HUD_COLOR_ROLE_SECONDARY_BORDER));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 10, 0, 10);
        panel.add(new JLabel(new SearchIcon()), gbc);
        return panel;
    }

    private static JPanel clearSegment(JButton clearButton) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(true);
        panel.setBackground(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND);
        panel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, HudPalette.HUD_COLOR_ROLE_SECONDARY_BORDER));
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
            // Focus accent (cyan) brightens the frame while editing; unfocused frame stays the dim
            // teal so it does not compete with the orange action buttons.
            g2.setColor(focused ? HudPalette.HUD_COLOR_ROLE_INFORMATION : HudPalette.HUD_COLOR_ROLE_FRAME_BORDER);
            if (variant == Variant.TABLE_FILTER) {
                g2.drawRect(0, 0, w - 1, h - 1);   // full enclosing border
            } else {
                // TABLE_FILTER_CONNECTED: top/left/right border only - the table provides the bottom
                g2.drawLine(0, 0, w - 1, 0);           // top
                g2.drawLine(0, 0, 0, h - 1);           // left
                g2.drawLine(w - 1, 0, w - 1, h - 1);  // right
                // Dim separator line at the bottom (marks the filter/table boundary)
                g2.setColor(HudPalette.HUD_COLOR_ROLE_SECONDARY_BORDER);
                g2.drawLine(0, h - 1, w - 1, h - 1);
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Paints the cyan corner accent marks on top of the child segments. They must be drawn after
     * {@code paintChildren} (here, post {@code super.paint}) because the opaque icon/clear segments
     * fill the panel interior and would otherwise overdraw marks placed inside the 1 px border inset.
     */
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (variant != Variant.TABLE_FILTER && variant != Variant.TABLE_FILTER_CONNECTED) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            // Marks lengthen slightly while focused, echoing the brighter frame.
            int m = focused ? CORNER_MARK + 2 : CORNER_MARK;
            g2.setColor(HudPalette.HUD_COLOR_ROLE_INFORMATION_MARK);
            // TABLE_FILTER shows a diagonal pair (top-left + bottom-right); the connected variant has
            // no bottom edge of its own, so it marks the two top corners instead.
            g2.drawLine(1, 1, 1 + m, 1);       g2.drawLine(1, 1, 1, 1 + m);           // TL
            if (variant == Variant.TABLE_FILTER) {
                g2.drawLine(w-2-m, h-2, w-2, h-2); g2.drawLine(w-2, h-2-m, w-2, h-2);    // BR
            } else {
                g2.drawLine(w-2-m, 1, w-2, 1);     g2.drawLine(w-2, 1, w-2, 1 + m);       // TR
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
                g2.setColor(HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT);
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
                g2.setColor(HudPalette.HUD_COLOR_ROLE_INFORMATION_MARK);
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
