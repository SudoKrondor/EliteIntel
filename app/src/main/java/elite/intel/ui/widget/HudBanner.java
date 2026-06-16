package elite.intel.ui.widget;
import static elite.intel.ui.theme.HudPalette.*;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.theme.HudGlyphs;

import javax.swing.*;
import javax.swing.border.Border;
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
        this(text, state, leadingWarnGlyph, false);
    }

    /**
     * Multiline banner whose text wraps to the available width instead of clipping. Use for long
     * informational hints in narrow columns (canon §7.3). Single-line glyph/warn banners stay on
     * the {@link #HudBanner(String, StatusBadge.State, boolean)} path.
     *
     * @param text  localized message text
     * @param state semantic state colour
     */
    public static HudBanner multiline(String text, StatusBadge.State state) {
        return new HudBanner(text, state, false, true);
    }

    /** Semantic state, kept so {@link #setEnabled(boolean)} can re-tint to the disabled tone. */
    private final StatusBadge.State state;
    /** The text carrier (label or wrapping area) whose colour follows the enabled state. */
    private final JComponent textComponent;

    private HudBanner(String text, StatusBadge.State state, boolean leadingWarnGlyph, boolean multiline) {
        super(new BorderLayout(), colorFor(state), Variant.FLAT);
        this.state = state;
        setBorder(railBorder(colorFor(state)));
        if (multiline) {
            JTextArea area = new JTextArea(text == null ? "" : text);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setEditable(false);
            area.setFocusable(false);
            area.setOpaque(false);
            area.setForeground(colorFor(state));
            // Proportional Label font (not the monospaced JTextArea default), at banner size.
            Font base = UIManager.getFont("Label.font");
            if (base == null) base = area.getFont();
            area.setFont(base.deriveFont(Font.BOLD, HudPalette.HUD_FONT_BANNER));
            area.putClientProperty("eliteIntel.hud.lockedForeground", Boolean.TRUE);
            this.textComponent = area;
        } else {
            JLabel label = new JLabel(text == null ? "" : text);
            label.setForeground(colorFor(state));
            label.setFont(label.getFont().deriveFont(Font.BOLD, HudPalette.HUD_FONT_BANNER));
            label.putClientProperty("eliteIntel.hud.lockedForeground", Boolean.TRUE);
            if (leadingWarnGlyph) {
                int glyph = Math.round(HudPalette.HUD_FONT_BANNER * 1.4f);
                label.setIcon(HudGlyphs.warningGlyphIcon(glyph));
                label.setIconTextGap(HudPalette.HUD_GAP);
            }
            this.textComponent = label;
        }
        add(textComponent, BorderLayout.CENTER);
    }

    private static Border railBorder(Color rail) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 2, 0, 0, rail),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)
        );
    }

    /** Dims the rail and text to {@code HUD_DISABLED} when disabled (§0.6), restores the state colour when enabled. */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        Color c = enabled ? colorFor(state) : HudPalette.HUD_DISABLED;
        if (textComponent != null) textComponent.setForeground(c);
        setBorder(railBorder(c));
        repaint();
    }

    private static Color colorFor(StatusBadge.State state) {
        if (state == null) return HudPalette.HUD_CYAN;
        return switch (state) {
            case OK      -> HudPalette.HUD_OK;
            case STANDBY -> HudPalette.HUD_WARN;
            case OFFLINE -> HudPalette.HUD_DANGER;
            case INFO    -> HudPalette.HUD_CYAN;
            case IDLE    -> HudPalette.HUD_DISABLED;
        };
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension preferred = getPreferredSize();
        return new Dimension(Integer.MAX_VALUE, preferred.height);
    }
}
