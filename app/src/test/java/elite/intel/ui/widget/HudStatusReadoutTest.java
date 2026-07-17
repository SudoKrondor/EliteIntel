package elite.intel.ui.widget;

import elite.intel.ui.theme.HudPalette;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudStatusReadoutTest {

    @Test
    void keepsCanonicalGapBetweenKeyAndValueAtPreferredWidth() throws Exception {
        onEdt(() -> {
            HudStatusReadout readout = new HudStatusReadout(
                    "Route", "Calculating route", StatusBadge.State.STANDBY);
            readout.setSize(readout.getPreferredSize());
            readout.doLayout();

            JLabel[] labels = Arrays.stream(readout.getComponents())
                    .filter(JLabel.class::isInstance)
                    .map(JLabel.class::cast)
                    .toArray(JLabel[]::new);

            assertEquals(2, labels.length);
            JLabel key = labels[0];
            JLabel value = labels[1];
            int actualGap = value.getX() - (key.getX() + key.getWidth());
            assertTrue(actualGap >= HudPalette.HUD_GAP,
                    () -> "Expected key/value gap >= " + HudPalette.HUD_GAP
                            + ", actual: " + actualGap);
        });
    }

    private static void onEdt(Runnable action)
            throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(action);
    }
}
