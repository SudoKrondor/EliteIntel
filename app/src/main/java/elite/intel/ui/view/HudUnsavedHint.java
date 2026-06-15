package elite.intel.ui.view;

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
        setForeground(AppTheme.HUD_WARN);
        setFont(getFont().deriveFont(Font.BOLD, AppTheme.HUD_FONT_BANNER));
        int glyph = Math.round(AppTheme.HUD_FONT_BANNER * 1.4f);
        setIcon(AppTheme.warningGlyphIcon(glyph));
        setIconTextGap(AppTheme.HUD_GAP / 2);
        putClientProperty(AppTheme.HUD_LOCKED_FOREGROUND, Boolean.TRUE);
        setVisible(false);
    }
}
