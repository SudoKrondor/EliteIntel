package elite.intel.ui.widget;

import elite.intel.ui.theme.HudGlyphs;
import elite.intel.ui.theme.HudPalette;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class HudCheckBoxTest {

    /**
     * The 14 px marker sits at the left edge, centred in the 26 px row.
     */
    private static final int MARKER_SIZE = 14;
    private static final int MARKER_Y = 6;

    @Test
    void nothingIsPaintedBehindTheLabel() {
        HudCheckBox box = new HudCheckBox("Announce", true);
        box.setSize(400, box.getPreferredSize().height);

        BufferedImage image = paint(box);

        // The label's glyphs stop short of the row's top edge, and past its end there is nothing at all.
        for (int x = 0; x < image.getWidth(); x++) {
            assertEquals(0, image.getRGB(x, 0) >>> 24, "painted at top edge, x=" + x);
        }
        assertEquals(0, image.getRGB(399, image.getHeight() / 2) >>> 24);
    }

    @Test
    void onlyTheMarkerAndLabelTakeTheMouseWhenAColumnStretchesTheControl() {
        HudCheckBox box = new HudCheckBox("Announce", false);
        int contentWidth = box.getPreferredSize().width;
        box.setSize(contentWidth + 300, box.getPreferredSize().height);

        assertTrue(box.contains(0, MARKER_Y));
        assertTrue(box.contains(contentWidth - 1, MARKER_Y));
        assertFalse(box.contains(contentWidth, MARKER_Y));
        assertFalse(box.contains(contentWidth + 299, MARKER_Y));
    }

    @Test
    void aCheckedMarkerIsFilledAndAnUncheckedOneIsOnlyOutlined() {
        HudCheckBox box = new HudCheckBox("Announce", true);
        box.setSize(box.getPreferredSize());
        int insideCorner = MARKER_Y + 3;

        assertTrue(paint(box).getRGB(3, insideCorner) >>> 24 > 0);

        box.setSelected(false);
        BufferedImage off = paint(box);
        assertEquals(0, off.getRGB(3, insideCorner) >>> 24);
        assertTrue(off.getRGB(MARKER_SIZE / 2, MARKER_Y) >>> 24 > 0, "the outline's top edge");
    }

    @Test
    void aDisabledCheckedToggleStillShowsItIsOn() {
        HudCheckBox box = new HudCheckBox("Announce", true);
        box.setEnabled(false);
        box.setSize(box.getPreferredSize());

        assertTrue(paint(box).getRGB(3, MARKER_Y + 3) >>> 24 > 0);
    }

    @Test
    void theTickIsPaintedInItsOwnColourSoItReadsAgainstTheFill() {
        // A selected table row inverts the marker, so a tick that followed the marker's colour would vanish.
        BufferedImage image = new BufferedImage(MARKER_SIZE, MARKER_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        HudGlyphs.paintHudCheckMarker(g, 0, 0, MARKER_SIZE, Color.BLACK, Color.WHITE, true);
        g.dispose();

        // The tick's lowest joint.
        assertEquals(Color.WHITE.getRGB(), image.getRGB(6, 9));
        assertEquals(Color.BLACK.getRGB(), image.getRGB(2, 2));
    }

    @Test
    void theInfoZoneFollowsTheLabelAndAClickThereRunsTheActionWithoutToggling() {
        HudCheckBox box = new HudCheckBox("Announce", false);
        int labelEnd = box.getPreferredSize().width;
        AtomicInteger infoRuns = new AtomicInteger();
        box.setInfoAction(infoRuns::incrementAndGet);
        int zoneWidth = box.getPreferredSize().width - labelEnd;
        assertEquals(HudPalette.HUD_TABLE_ROW_HEIGHT_COMPACT, zoneWidth);
        box.setSize(box.getPreferredSize().width + 300, box.getPreferredSize().height);

        int x = labelEnd + zoneWidth / 2;
        box.processMouseEvent(new MouseEvent(box, MouseEvent.MOUSE_CLICKED, 0, 0, x, MARKER_Y, 1, false, MouseEvent.BUTTON1));

        assertEquals(1, infoRuns.get());
        assertFalse(box.isSelected());
        assertTrue(box.contains(x, MARKER_Y));
    }

    private static BufferedImage paint(Component component) {
        BufferedImage image = new BufferedImage(
                component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        component.paint(g);
        g.dispose();
        return image;
    }
}
