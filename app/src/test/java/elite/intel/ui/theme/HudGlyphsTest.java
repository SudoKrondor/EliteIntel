package elite.intel.ui.theme;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the visual-bounds alignment contract for shared HUD text carets. */
class HudGlyphsTest {

    @Test
    void textCaretCentersOnThePromptChevron() {
        BufferedImage image = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        Color caretColor = new Color(73, 193, 103);
        int baseline = 48;
        try {
            Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 16);
            g2.setFont(font);
            FontMetrics metrics = g2.getFontMetrics();
            GlyphVector marker = font.createGlyphVector(g2.getFontRenderContext(), "»");
            Rectangle2D markerBounds = marker.getVisualBounds();

            HudGlyphs.paintHudTextCaret(g2, 20, baseline, metrics, caretColor);

            Rectangle caretBounds = paintedBounds(image, caretColor);
            assertNotNull(caretBounds);
            assertEquals(HudPalette.HUD_CARET_WIDTH, caretBounds.width);
            assertEquals(Math.max(1, metrics.getAscent() - metrics.getDescent()), caretBounds.height);
            double caretCenter = caretBounds.y + caretBounds.height / 2.0;
            double markerCenter = baseline + markerBounds.getCenterY();
            assertTrue(Math.abs(caretCenter - markerCenter) <= 0.5,
                    "caret center must match the prompt marker center");
        } finally {
            g2.dispose();
        }
    }

    private static Rectangle paintedBounds(BufferedImage image, Color color) {
        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) != color.getRGB()) {
                    continue;
                }
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        return maxX < 0 ? null : new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }
}
