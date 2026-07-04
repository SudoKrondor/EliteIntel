package elite.intel.ui.widget;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudGlyphs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static elite.intel.ui.theme.AppTheme.*;
import static elite.intel.ui.theme.HudGlyphs.*;
import static elite.intel.ui.theme.HudPalette.*;

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
        setBackground(HUD_COLOR_ROLE_DIALOG_HEADER_BACKGROUND);
        setPreferredSize(new Dimension(0, HUD_DIALOG_HEADER_HEIGHT));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, HUD_BORDER_THICKNESS_ACCENT, 0, HUD_COLOR_ROLE_PRIMARY_ACTION),
                BorderFactory.createEmptyBorder(0, HUD_PADDING, 0, HUD_PADDING)));
        putClientProperty(HUD_LOCKED_FOREGROUND, Boolean.TRUE);

        // Icon (left) - elite-logo at nav size, dimmed to 55 %
        ImageIcon logo = null;
        try {
            logo = scaledIcon(HudDialogHeader.class, "/images/elite-logo.png", HUD_ICON_NAV);
            logo = tintIcon(logo, HUD_ICON_NAV, HUD_ICON_NAV, HUD_COLOR_ROLE_CONTROL_DECORATION);
        } catch (Exception ignored) {}
        JLabel iconLabel = new JLabel(logo);

        // Title
        JLabel titleLabel = new JLabel(title != null ? title.toUpperCase() : "");
        titleLabel.setForeground(HUD_COLOR_ROLE_DIALOG_TITLE_TEXT);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, HUD_FONT_APP_TITLE));
        titleLabel.putClientProperty(HUD_LOCKED_FOREGROUND, Boolean.TRUE);

        add(iconLabel, BorderLayout.WEST);
        add(titleLabel, BorderLayout.CENTER);
        add(new HudGlyphButton(HudGlyphs::paintHudCloseGlyph,
                HUD_COLOR_ROLE_CONTROL_DECORATION, HUD_COLOR_ROLE_DANGER, null, onClose), BorderLayout.EAST);

        // Window drag - registered on the panel so empty areas and both labels forward events here
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
}
