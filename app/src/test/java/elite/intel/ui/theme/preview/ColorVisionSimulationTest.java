package elite.intel.ui.theme.preview;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The expected colours are the ones the same model gave when the Default theme was tuned for colour blindness
 * (2026-11-14), so View as shows the commander what that tuning was measured against.
 */
class ColorVisionSimulationTest {

    @Test
    void normalVisionLeavesThePictureAlone() {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);

        assertSame(image, ColorVisionSimulation.NORMAL.apply(image));
    }

    /** The old Default green and red: two olives a deuteranope can barely tell apart. */
    @Test
    void deuteranopiaTurnsTheOldGreenAndRedIntoOlives() {
        assertNear(0xB7AA71, ColorVisionSimulation.DEUTERANOPIA.apply(0x4FC56B));
        assertNear(0x96894B, ColorVisionSimulation.DEUTERANOPIA.apply(0xD94F4F));
    }

    @Test
    void protanopiaDarkensRed() {
        assertNear(0x736C4E, ColorVisionSimulation.PROTANOPIA.apply(0xD94F4F));
    }

    @Test
    void tritanopiaShiftsBlueTowardCyan() {
        assertNear(0x00BBC7, ColorVisionSimulation.TRITANOPIA.apply(0x6EA7FF));
    }

    private static void assertNear(int expected, int actual) {
        for (int shift = 0; shift <= 16; shift += 8) {
            int e = expected >> shift & 0xFF;
            int a = actual >> shift & 0xFF;
            assertTrue(Math.abs(e - a) <= 1, String.format("expected %06X, got %06X", expected, actual));
        }
        assertEquals(expected >>> 24, actual >>> 24);
    }
}
