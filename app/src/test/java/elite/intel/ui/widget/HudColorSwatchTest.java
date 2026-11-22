package elite.intel.ui.widget;

import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudColorSwatchTest {

    @Test
    void itPaintsTheColourItHolds() {
        HudColorSwatch swatch = new HudColorSwatch(Color.RED);
        swatch.setSize(swatch.getPreferredSize());
        assertEquals(Color.RED.getRGB(), centrePixel(swatch));

        swatch.setColor(Color.BLUE);

        assertEquals(Color.BLUE, swatch.getColor());
        assertEquals(Color.BLUE.getRGB(), centrePixel(swatch));
    }

    private static int centrePixel(Component component) {
        BufferedImage image = new BufferedImage(
                component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        component.paint(g);
        g.dispose();
        return image.getRGB(image.getWidth() / 2, image.getHeight() / 2);
    }
}
