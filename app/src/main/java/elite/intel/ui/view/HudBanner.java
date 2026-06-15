package elite.intel.ui.view;

import javax.swing.*;
import java.awt.*;

/**
 * Reusable compact banner for warnings, informational messages, and status hints.
 */
public class HudBanner extends HudPanel {

    /**
     * Creates a banner with semantic colour treatment.
     *
     * @param text localized message text
     * @param state semantic state colour
     */
    public HudBanner(String text, StatusBadge.State state) {
        this(text, state, false);
    }

    /**
     * Creates a banner with an optional leading warning glyph. Use {@code leadingWarnGlyph=true}
     * for caution hints (replaces hand-rolled "⚠" warning strips) so they render consistently.
     *
     * @param text            localized message text
     * @param state           semantic state colour
     * @param leadingWarnGlyph draw the HUD warning glyph (§13) before the text
     */
    public HudBanner(String text, StatusBadge.State state, boolean leadingWarnGlyph) {
        super(new BorderLayout(), colorFor(state), Variant.FLAT);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 2, 0, 0, colorFor(state)),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        JLabel label = new JLabel(text == null ? "" : text);
        label.setForeground(colorFor(state));
        label.setFont(label.getFont().deriveFont(Font.BOLD, AppTheme.HUD_FONT_BANNER));
        label.putClientProperty("eliteIntel.hud.lockedForeground", Boolean.TRUE);
        if (leadingWarnGlyph) {
            int glyph = Math.round(AppTheme.HUD_FONT_BANNER * 1.4f);
            label.setIcon(AppTheme.warningGlyphIcon(glyph));
            label.setIconTextGap(AppTheme.HUD_GAP);
        }
        add(label, BorderLayout.CENTER);
    }

    private static Color colorFor(StatusBadge.State state) {
        if (state == null) return AppTheme.HUD_CYAN;
        return switch (state) {
            case OK      -> AppTheme.HUD_OK;
            case STANDBY -> AppTheme.HUD_WARN;
            case OFFLINE -> AppTheme.HUD_DANGER;
            case INFO    -> AppTheme.HUD_CYAN;
            case IDLE    -> AppTheme.HUD_DISABLED;
        };
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension preferred = getPreferredSize();
        return new Dimension(Integer.MAX_VALUE, preferred.height);
    }
}
