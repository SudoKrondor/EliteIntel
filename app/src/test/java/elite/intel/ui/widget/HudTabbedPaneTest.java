package elite.intel.ui.widget;

import com.formdev.flatlaf.FlatLightLaf;
import elite.intel.ui.theme.HudPalette;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The active tab's box sits on the rail under the row, with nothing between them. Read back from the painted pixels.
 */
class HudTabbedPaneTest {

    private static final int RAIL_MAIN_NAV = 3;
    private static final int RAIL_SECTION = 2;

    @BeforeAll
    static void lookAndFeel() {
        FlatLightLaf.setup();
    }

    @Test
    void theMainNavBoxSitsOnTheRail() {
        assertBoxSitsOnRail(HudTabbedPane.Level.MAIN_NAV, RAIL_MAIN_NAV, HudPalette.HUD_COLOR_ROLE_MAIN_TAB_ACTIVE_BACKGROUND);
    }

    @Test
    void theSectionBoxSitsOnTheRail() {
        assertBoxSitsOnRail(HudTabbedPane.Level.SECTION, RAIL_SECTION, HudPalette.HUD_COLOR_ROLE_SECTION_TAB_ACTIVE_BACKGROUND);
    }

    private static void assertBoxSitsOnRail(HudTabbedPane.Level level, int rail, Color box) {
        HudTabbedPane tabs = new HudTabbedPane(level);
        tabs.addTab("one", new JPanel());
        tabs.addTab("two", new JPanel());
        tabs.setSize(400, 200);
        layOut(tabs);
        BufferedImage painted = new BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = painted.createGraphics();
        try {
            tabs.paint(g);
        } finally {
            g.dispose();
        }

        // WHY the second tab's bounds: the selected tab's are padded, the row's real bottom is the unselected one's.
        Rectangle selected = tabs.getBoundsAt(0);
        Rectangle unselected = tabs.getBoundsAt(1);
        int lastRowAboveRail = unselected.y + unselected.height - rail - 1;

        PaintedColours.assertInkOver(box, HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND, painted, selected.x + 3, lastRowAboveRail);
        assertEquals(HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND.getRGB(), painted.getRGB(unselected.x + 3, lastRowAboveRail));
    }

    private static void layOut(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) {
                layOut(nested);
            }
        }
    }
}
