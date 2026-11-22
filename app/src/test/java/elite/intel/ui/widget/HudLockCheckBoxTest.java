package elite.intel.ui.widget;

import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

import static elite.intel.ui.i18n.MultiLingualTextProvider.getText;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HudLockCheckBoxTest {

    @Test
    void theTooltipSaysWhatThePadlockShowsHoweverTheStateChanges() {
        HudLockCheckBox lock = new HudLockCheckBox(true);
        assertEquals(getText("settings.cloud.locked"), lock.getToolTipText());

        // The API key panel unlocks its field from code as well as from a click.
        lock.setSelected(false);
        assertEquals(getText("settings.cloud.unlocked"), lock.getToolTipText());

        lock.setSelected(true);
        assertEquals(getText("settings.cloud.locked"), lock.getToolTipText());
    }

    @Test
    void itPaintsADifferentPadlockForEachState() {
        HudLockCheckBox lock = new HudLockCheckBox(true);
        lock.setSize(lock.getPreferredSize());
        int[] locked = pixels(lock);

        lock.setSelected(false);

        assertFalse(Arrays.equals(locked, pixels(lock)));
    }

    @Test
    void aDisabledLockStillPaintsItsPadlockGreyedOut() {
        HudLockCheckBox lock = new HudLockCheckBox(true);
        lock.setSize(lock.getPreferredSize());
        int[] enabled = pixels(lock);

        lock.setEnabled(false);

        assertFalse(Arrays.equals(enabled, pixels(lock)));
    }

    private static int[] pixels(Component component) {
        BufferedImage image = new BufferedImage(
                component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        component.paint(g);
        g.dispose();
        return ((DataBufferInt) image.getRaster().getDataBuffer()).getData().clone();
    }
}
