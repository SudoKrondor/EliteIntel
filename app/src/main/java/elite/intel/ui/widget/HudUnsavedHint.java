package elite.intel.ui.widget;
import static elite.intel.ui.theme.HudPalette.*;

import elite.intel.ui.theme.AppTheme;
import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.theme.HudGlyphs;

import elite.intel.ui.i18n.MultiLingualTextProvider;

import javax.swing.*;
import java.awt.*;

/**
 * Standard footer "unsaved changes" status (§10): {@code HUD_WARN} caution text with a leading
 * ⚠ glyph, hidden by default. Place it just left of a SAVE button (grouped at the right edge) and
 * toggle visibility with the screen's dirty flag, so every SAVE footer reads the same.
 */
public class HudUnsavedHint extends JLabel {

    public HudUnsavedHint() {
        super(MultiLingualTextProvider.getText("status.unsavedChanges"));
        setForeground(HudPalette.HUD_WARN);
        setFont(getFont().deriveFont(Font.BOLD, HudPalette.HUD_FONT_BANNER));
        int glyph = Math.round(HudPalette.HUD_FONT_BANNER * 1.4f);
        setIcon(HudGlyphs.warningGlyphIcon(glyph));
        setIconTextGap(HudPalette.HUD_GAP / 2);
        putClientProperty(AppTheme.HUD_LOCKED_FOREGROUND, Boolean.TRUE);
        setVisible(false);
    }
}
