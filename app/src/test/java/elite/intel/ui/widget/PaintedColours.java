package elite.intel.ui.widget;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Reads a palette colour back from painted pixels, whether or not that colour is opaque.
 * <p>
 * A translucent role never appears on screen as itself: the pixel holds the role blended over whatever was underneath.
 * Each repaint of the same pixel blends again, and a control that repaints its corner marks over its children does
 * exactly that, so the expected value is any number of passes rather than one.
 */
final class PaintedColours {

    /**
     * WHY a limit: at the palette's alpha values a pixel has all but reached the ink itself by this many passes.
     */
    private static final int MAX_PASSES = 6;
    /**
     * WHY a tolerance: Java2D rounds each blend, and over several passes the rounding adds up.
     */
    private static final int CHANNEL_TOLERANCE = 3;

    private PaintedColours() {
    }

    static void assertInkOver(Color ink, Color surface, BufferedImage image, int x, int y) {
        int actual = image.getRGB(x, y);
        double alpha = ink.getAlpha() / 255.0;
        double red = surface.getRed();
        double green = surface.getGreen();
        double blue = surface.getBlue();
        for (int pass = 1; pass <= MAX_PASSES; pass++) {
            red = ink.getRed() * alpha + red * (1 - alpha);
            green = ink.getGreen() * alpha + green * (1 - alpha);
            blue = ink.getBlue() * alpha + blue * (1 - alpha);
            if (close(red, actual >> 16) && close(green, actual >> 8) && close(blue, actual)) {
                return;
            }
        }
        fail("pixel (" + x + "," + y + ") is #" + Integer.toHexString(actual)
                + ", which is not #" + Integer.toHexString(ink.getRGB())
                + " painted over #" + Integer.toHexString(surface.getRGB()));
    }

    private static boolean close(double expected, int actualChannel) {
        return Math.abs(expected - (actualChannel & 0xFF)) <= CHANNEL_TOLERANCE;
    }
}
