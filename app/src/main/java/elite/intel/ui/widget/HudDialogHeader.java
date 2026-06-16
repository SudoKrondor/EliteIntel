package elite.intel.ui.widget;

import elite.intel.ui.theme.AppTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static elite.intel.ui.theme.AppTheme.*;

/**
 * Reusable HUD dialog header strip: muted Elite logo on the left, title in caps,
 * and a close-glyph button on the right. Drag anywhere on the header (except the
 * close button) to move the owning undecorated window.
 */
public class HudDialogHeader extends JPanel {

    private final Point dragOffset = new Point();

    /**
     * @param title   dialog title; rendered in upper case
     * @param onClose called when the close glyph is clicked
     */
    public HudDialogHeader(String title, Runnable onClose) {
        setLayout(new BorderLayout(HUD_PADDING, 0));
        setOpaque(true);
        setBackground(HUD_DIALOG_HEADER_BG);
        setPreferredSize(new Dimension(0, HUD_DIALOG_HEADER_HEIGHT));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, HUD_BORDER_THICKNESS_ACCENT, 0, ACCENT),
                BorderFactory.createEmptyBorder(0, HUD_PADDING, 0, HUD_PADDING)));
        putClientProperty(HUD_LOCKED_FOREGROUND, Boolean.TRUE);

        // Icon (left) — elite-logo at nav size, dimmed to 55 %
        ImageIcon logo = null;
        try {
            logo = scaledIcon(HudDialogHeader.class, "/images/elite-logo.png", HUD_ICON_NAV);
            logo = tintIcon(logo, HUD_ICON_NAV, HUD_ICON_NAV, HUD_ORANGE_SOFT);
        } catch (Exception ignored) {}
        JLabel iconLabel = new JLabel(logo);

        // Title
        JLabel titleLabel = new JLabel(title != null ? title.toUpperCase() : "");
        titleLabel.setForeground(HUD_DIALOG_TITLE_FG);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, HUD_FONT_APP_TITLE));
        titleLabel.putClientProperty(HUD_LOCKED_FOREGROUND, Boolean.TRUE);

        add(iconLabel, BorderLayout.WEST);
        add(titleLabel, BorderLayout.CENTER);
        add(new CloseGlyphButton(onClose), BorderLayout.EAST);

        // Window drag — registered on the panel so empty areas and both labels forward events here
        MouseAdapter drag = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragOffset.setLocation(e.getPoint());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Window w = SwingUtilities.getWindowAncestor(HudDialogHeader.this);
                if (w != null) w.setLocation(
                        e.getXOnScreen() - dragOffset.x,
                        e.getYOnScreen() - dragOffset.y);
            }
        };
        addMouseListener(drag);
        addMouseMotionListener(drag);
    }

    // -------------------------------------------------------------------------
    // Close glyph button (icon-only, lightweight)
    // -------------------------------------------------------------------------

    private static final class CloseGlyphButton extends JComponent {

        private final Runnable onClose;
        private boolean hover;

        CloseGlyphButton(Runnable onClose) {
            this.onClose = onClose;
            setOpaque(false);
            setPreferredSize(new Dimension(HUD_TABLE_ROW_HEIGHT_COMPACT, HUD_TABLE_ROW_HEIGHT_COMPACT));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                private boolean armed = false;

                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    armed = false;
                    hover = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        armed = true;
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (armed && SwingUtilities.isLeftMouseButton(e)
                            && contains(e.getPoint())) {
                        armed = false;
                        onClose.run();
                    } else {
                        armed = false;
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                Color tint = hover ? HUD_DANGER : HUD_ORANGE_SOFT;
                int gs = HUD_ICON_TABLE;
                int gx = (getWidth()  - gs) / 2;
                int gy = (getHeight() - gs) / 2;
                AppTheme.paintHudCloseGlyph(g2, gx, gy, gs, gs, tint);
            } finally {
                g2.dispose();
            }
        }
    }
}
