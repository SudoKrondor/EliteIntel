package elite.intel.ui.widget;

import elite.intel.ui.i18n.MultiLingualTextProvider;
import elite.intel.ui.theme.HudGlyphs;
import elite.intel.ui.theme.HudPalette;

import javax.swing.*;
import java.awt.*;

/**
 * The checkbox that guards a field against stray edits: a {@link HudCheckBox} with a padlock in place of its
 * label, closed while checked and open while not. The words the picture replaces are the tooltip, which follows
 * the state however it changes.
 */
public class HudLockCheckBox extends HudCheckBox {

    private static final int ICON_SIZE = HudPalette.HUD_ICON_TABLE;

    private final Icon locked = padlock("/images/lock.png", HudPalette.HUD_COLOR_ROLE_PRIMARY_ACTION);
    private final Icon unlocked = padlock("/images/unlock.png", HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION);
    private final Icon lockedDisabled = padlock("/images/lock.png", HudPalette.HUD_COLOR_ROLE_DISABLED);
    private final Icon unlockedDisabled = padlock("/images/unlock.png", HudPalette.HUD_COLOR_ROLE_DISABLED);

    public HudLockCheckBox(boolean selected) {
        super(null, selected);
        describeState();
        addItemListener(e -> describeState());
    }

    @Override
    protected int labelWidth() {
        return ICON_SIZE;
    }

    @Override
    protected void paintLabel(Graphics2D g2, int x, int h, Color textColor) {
        Icon icon = isEnabled()
                ? (isSelected() ? locked : unlocked)
                : (isSelected() ? lockedDisabled : unlockedDisabled);
        icon.paintIcon(this, g2, x, (h - ICON_SIZE) / 2);
    }

    private void describeState() {
        setToolTipText(MultiLingualTextProvider.getText(
                isSelected() ? "settings.cloud.locked" : "settings.cloud.unlocked"));
    }

    private static Icon padlock(String resource, Color color) {
        ImageIcon glyph = HudGlyphs.scaledIcon(HudLockCheckBox.class, resource, ICON_SIZE);
        return HudGlyphs.tintIcon(glyph, ICON_SIZE, ICON_SIZE, color);
    }
}
