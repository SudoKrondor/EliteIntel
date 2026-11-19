package elite.intel.ui.screen.bindings;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * The reconcile wizard's own pieces (multi-install-reconcile.md): the choice card a question is answered with, the
 * radio chip of the button-by-button view, and wrapped text. Every colour is a {@link HudPalette} role; a selection
 * takes the accent fill, which the HUD canon allows for a selected item, and state stays text colour.
 */
final class ReconcileWidgets {

    private static final int RADIO = 18;
    private static final int CHIP_DOT = 13;

    private ReconcileWidgets() {
    }

    /**
     * Text that wraps at {@code width} pixels: a text area that cannot be edited, selected or focused, and passes a click
     * through to the card or chip it sits in, so the whole card answers the question.
     */
    static JComponent wrapped(String text, Color color, float size, int width) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setFocusable(false);
        area.setHighlighter(null);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setBorder(null);
        area.setForeground(color);
        area.setFont(UIManager.getFont("Label.font").deriveFont(size));
        // WHY: a wrapping text area reports one line until it knows its width - given the width, it gives the height.
        area.setSize(new Dimension(width, Short.MAX_VALUE));
        Dimension wanted = new Dimension(width, area.getPreferredSize().height);
        area.setPreferredSize(wanted);
        area.setMaximumSize(wanted);
        area.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                AbstractButton owner = (AbstractButton) SwingUtilities.getAncestorOfClass(AbstractButton.class, area);
                if (owner != null && owner.isEnabled()) owner.doClick();
            }
        });
        area.setCursor(null);
        return area;
    }

    /** A line of text in one colour and size, not wrapped. */
    static JLabel line(String text, Color color, float size, int style) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(label.getFont().deriveFont(style, size));
        return label;
    }

    /** A name the bindings use, in the monospace face, so it reads as a name and not as a word. */
    static JLabel alias(String name, Color color, float size) {
        JLabel label = new JLabel(name);
        Font mono = UIManager.getFont("monospaceFont");
        label.setFont((mono != null ? mono : label.getFont()).deriveFont(Font.BOLD, size));
        label.setForeground(color);
        return label;
    }

    /** A copy of the game as a plain text badge - its storefront, never a logo. */
    static JLabel badge(String text, boolean ours) {
        JLabel label = new JLabel(text.toUpperCase());
        label.setFont(label.getFont().deriveFont(Font.BOLD, HudPalette.HUD_FONT_XS));
        Color color = ours ? HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION : HudPalette.HUD_COLOR_ROLE_SECONDARY_TEXT;
        label.setForeground(color);
        label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(
                ours ? color : HudPalette.HUD_COLOR_ROLE_FRAME_BORDER), new EmptyBorder(2, 6, 2, 6)));
        return label;
    }

    /** A link-like button: accent text, no box. */
    static JButton link(String text) {
        JButton button = new JButton("<html><u>" + html(text) + "</u></html>");
        button.setForeground(HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION);
        button.setFont(button.getFont().deriveFont(HudPalette.HUD_FONT_SM));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    /**
     * A page for a scroll pane: as wide as the view, as tall as its content - so a short page sits at the top rather than
     * being stretched to fill the window.
     */
    static JComponent scrollablePage(JComponent content) {
        JPanel page = new TrackingPanel();
        page.add(content, BorderLayout.NORTH);
        return page;
    }

    private static final class TrackingPanel extends JPanel implements Scrollable {
        TrackingPanel() {
            super(new BorderLayout());
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(16, visible.height - 32);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /** A panel stacking its children top to bottom, left-aligned, transparent - fill it with {@link #stack}. */
    static JPanel column(int gap) {
        JPanel panel = AppTheme.transparentPanel(null);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.putClientProperty("gap", gap);
        return panel;
    }

    /** Adds {@code child} to a {@link #column}, left-aligned, with the column's gap above all but the first. */
    static void stack(JPanel column, JComponent child) {
        Object gap = column.getClientProperty("gap");
        if (column.getComponentCount() > 0 && gap instanceof Integer pixels && pixels > 0) {
            column.add(Box.createVerticalStrut(pixels));
        }
        child.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(child);
    }

    /** Text safe inside a label's HTML. */
    static String html(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    }

    /**
     * One answer to a question, as a big bordered card with a radio mark in its corner: what it is, where it comes
     * from, and what happens if it is picked. Selected, it takes the accent border and fill. In a {@link ButtonGroup}
     * with no selection to start with - nothing is ever chosen for the player (multi-install-reconcile.md, D6).
     * <p>
     * Enter does not press it: the dialog's Enter goes to Next, which waits until a card is chosen (Storage S10's
     * rule that Enter chooses nothing).
     */
    static final class ChoiceCard extends JToggleButton {
        private final JPanel content;

        ChoiceCard() {
            setLayout(new BorderLayout());
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setBorder(new EmptyBorder(16, 16, 14, 16 + RADIO + 8));
            content = column(6);
            add(content, BorderLayout.NORTH);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addItemListener(e -> repaint());
        }

        /** Adds a line to the card's content, top to bottom. */
        ChoiceCard with(JComponent line) {
            ReconcileWidgets.stack(content, line);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean on = isSelected();
            boolean hover = getModel().isRollover() && isEnabled();
            g2.setColor(on ? HudPalette.HUD_COLOR_ROLE_ACCENT_FILL
                    : hover ? HudPalette.HUD_COLOR_ROLE_TABLE_CELL_HOVER_BACKGROUND
                    : HudPalette.HUD_COLOR_ROLE_TABLE_CELL_BACKGROUND);
            if (on) g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setComposite(AlphaComposite.SrcOver);
            int thickness = on ? 2 : 1;
            g2.setColor(on ? HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION
                    : hasFocus() ? HudPalette.HUD_COLOR_ROLE_INPUT_FOCUS : HudPalette.HUD_COLOR_ROLE_FRAME_BORDER);
            g2.setStroke(new BasicStroke(thickness));
            g2.drawRect(thickness / 2, thickness / 2, getWidth() - thickness, getHeight() - thickness);
            paintRadio(g2, getWidth() - 16 - RADIO, 16, RADIO, on);
            g2.dispose();
        }
    }

    /**
     * One button's name in the button-by-button view: a small radio chip. Chosen, it is bordered and filled with the
     * accent; otherwise outlined - the old boxed labels looked the same chosen or not.
     */
    static final class Chip extends JToggleButton {
        Chip(String text) {
            super(text);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setHorizontalAlignment(SwingConstants.LEFT);
            setForeground(HudPalette.HUD_COLOR_ROLE_PRIMARY_TEXT);
            setFont(getFont().deriveFont(HudPalette.HUD_FONT_SM));
            setBorder(new EmptyBorder(6, 12 + CHIP_DOT + 8, 6, 12));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addItemListener(e -> repaint());
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            return new Dimension(Math.max(size.width, 180), size.height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean on = isSelected();
            if (on) {
                g2.setColor(HudPalette.HUD_COLOR_ROLE_ACCENT_FILL);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setComposite(AlphaComposite.SrcOver);
            }
            g2.setColor(on ? HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION : HudPalette.HUD_COLOR_ROLE_FRAME_BORDER);
            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            paintRadio(g2, 12, (getHeight() - CHIP_DOT) / 2, CHIP_DOT, on);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static void paintRadio(Graphics2D g2, int x, int y, int size, boolean on) {
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(on ? HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION : HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION);
        g2.draw(new Ellipse2D.Float(x + 1, y + 1, size - 2, size - 2));
        if (on) {
            int inset = Math.max(3, size / 4);
            g2.fill(new Ellipse2D.Float(x + inset, y + inset, size - 2 * inset, size - 2 * inset));
        }
    }
}
