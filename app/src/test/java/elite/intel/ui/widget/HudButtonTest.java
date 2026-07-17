package elite.intel.ui.widget;

import elite.intel.ui.theme.HudPalette;
import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudButtonTest {

    @Test
    void infoZoneCanBeAddedAndRemovedWithoutChangingBaselineFootprint() throws Exception {
        onEdt(() -> {
            HudButton button = new HudButton("Calculate", false);
            Dimension baselineSize = button.getPreferredSize();
            Insets baselineInsets = button.getInsets();

            assertFalse(button.hasInfoZone());

            button.setInfoAction(() -> { });

            assertTrue(button.hasInfoZone());
            assertEquals(baselineSize.width + HudPalette.HUD_SEP_W
                            + HudPalette.HUD_TABLE_ROW_HEIGHT_COMPACT,
                    button.getPreferredSize().width);
            assertEquals(baselineSize.height, button.getPreferredSize().height);

            button.setInfoAction(null);

            assertFalse(button.hasInfoZone());
            assertEquals(baselineSize, button.getPreferredSize());
            assertEquals(baselineInsets, button.getInsets());
        });
    }

    @Test
    void infoSeparatorKeepsOuterBorderContinuous() throws Exception {
        onEdt(() -> {
            HudButton button = new HudButton("Cancel route", false);
            button.setInfoAction(() -> { });
            button.setSize(button.getPreferredSize());

            BufferedImage image = paint(button);
            int separatorX = button.getWidth()
                    - HudPalette.HUD_TABLE_ROW_HEIGHT_COMPACT
                    - HudPalette.HUD_SEP_W;
            int expectedBorder = HudPalette.HUD_COLOR_ROLE_CONTROL_DECORATION.getRGB();
            int expectedGap = HudPalette.HUD_COLOR_ROLE_APPLICATION_BACKGROUND.getRGB();

            for (int x = separatorX; x < separatorX + HudPalette.HUD_SEP_W; x++) {
                assertEquals(expectedBorder, image.getRGB(x, 0));
                assertEquals(expectedBorder, image.getRGB(x, button.getHeight() - 1));
                assertEquals(expectedGap,
                        image.getRGB(x, HudPalette.HUD_BORDER_THICKNESS));
            }
        });
    }

    @Test
    void infoClickRunsOnlyInfoAction() throws Exception {
        onEdt(() -> {
            AtomicInteger mainCalls = new AtomicInteger();
            AtomicInteger infoCalls = new AtomicInteger();
            HudButton button = sizedButton(mainCalls, infoCalls);

            click(button, button.getWidth() - 1);

            assertEquals(0, mainCalls.get());
            assertEquals(1, infoCalls.get());
        });
    }

    @Test
    void mouseAndKeyboardStyleActivationRemainAttachedToMainAction() throws Exception {
        onEdt(() -> {
            AtomicInteger mainCalls = new AtomicInteger();
            AtomicInteger infoCalls = new AtomicInteger();
            HudButton button = sizedButton(mainCalls, infoCalls);

            click(button, 1);
            activateSpaceBinding(button);

            assertEquals(2, mainCalls.get());
            assertEquals(0, infoCalls.get());
        });
    }

    @Test
    void releasingOverInfoZoneCancelsAnArmedMainClick() throws Exception {
        onEdt(() -> {
            AtomicInteger mainCalls = new AtomicInteger();
            AtomicInteger infoCalls = new AtomicInteger();
            HudButton button = sizedButton(mainCalls, infoCalls);
            int y = button.getHeight() / 2;

            mouse(button, MouseEvent.MOUSE_ENTERED, 1, y, 0);
            mouse(button, MouseEvent.MOUSE_PRESSED, 1, y, InputEvent.BUTTON1_DOWN_MASK);
            mouse(button, MouseEvent.MOUSE_RELEASED, button.getWidth() - 1, y, 0);

            assertEquals(0, mainCalls.get());
            assertEquals(0, infoCalls.get());
        });
    }

    @Test
    void mainActionCanBeDisabledWhileInfoRemainsAvailable() throws Exception {
        onEdt(() -> {
            AtomicInteger mainCalls = new AtomicInteger();
            AtomicInteger infoCalls = new AtomicInteger();
            HudButton button = sizedButton(mainCalls, infoCalls);
            button.setMainActionEnabled(false);

            click(button, 1);
            click(button, button.getWidth() - 1);
            button.doClick(0);

            assertEquals(0, mainCalls.get());
            assertEquals(1, infoCalls.get());
            assertTrue(button.isEnabled());
            assertFalse(button.isMainActionEnabled());
        });
    }

    @Test
    void fullyDisabledButtonRunsNeitherAction() throws Exception {
        onEdt(() -> {
            AtomicInteger mainCalls = new AtomicInteger();
            AtomicInteger infoCalls = new AtomicInteger();
            HudButton button = sizedButton(mainCalls, infoCalls);
            button.setEnabled(false);

            click(button, 1);
            click(button, button.getWidth() - 1);
            button.doClick(0);

            assertEquals(0, mainCalls.get());
            assertEquals(0, infoCalls.get());
        });
    }

    @Test
    void removingInfoZoneRestoresOriginalPaint() throws Exception {
        onEdt(() -> {
            HudButton button = new HudButton("Cancel", false);
            button.setSize(button.getPreferredSize());
            BufferedImage baseline = paint(button);

            button.setInfoAction(() -> { });
            button.setInfoAction(null);
            button.setSize(button.getPreferredSize());

            assertImagesEqual(baseline, paint(button));
        });
    }

    private static HudButton sizedButton(AtomicInteger mainCalls, AtomicInteger infoCalls) {
        HudButton button = new HudButton("Route", false);
        button.addActionListener(e -> mainCalls.incrementAndGet());
        button.setInfoAction(infoCalls::incrementAndGet);
        button.setSize(button.getPreferredSize());
        return button;
    }

    private static void click(HudButton button, int x) {
        int y = button.getHeight() / 2;
        mouse(button, MouseEvent.MOUSE_ENTERED, x, y, 0);
        mouse(button, MouseEvent.MOUSE_PRESSED, x, y, InputEvent.BUTTON1_DOWN_MASK);
        mouse(button, MouseEvent.MOUSE_RELEASED, x, y, 0);
        mouse(button, MouseEvent.MOUSE_CLICKED, x, y, 0);
        mouse(button, MouseEvent.MOUSE_EXITED, x, y, 0);
    }

    private static void mouse(HudButton button, int id, int x, int y, int modifiers) {
        button.dispatchEvent(new MouseEvent(button, id, System.currentTimeMillis(), modifiers,
                x, y, 1, false, MouseEvent.BUTTON1));
    }

    private static void activateSpaceBinding(HudButton button) {
        Object pressedKey = button.getInputMap(JComponent.WHEN_FOCUSED)
                .get(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false));
        Object releasedKey = button.getInputMap(JComponent.WHEN_FOCUSED)
                .get(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true));
        assertNotNull(pressedKey);
        assertNotNull(releasedKey);
        Action pressed = button.getActionMap().get(pressedKey);
        Action released = button.getActionMap().get(releasedKey);
        assertNotNull(pressed);
        assertNotNull(released);
        ActionEvent event = new ActionEvent(button, ActionEvent.ACTION_PERFORMED, "space");
        pressed.actionPerformed(event);
        released.actionPerformed(event);
    }

    private static BufferedImage paint(HudButton button) {
        BufferedImage image = new BufferedImage(button.getWidth(), button.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        try {
            button.paint(g2);
        } finally {
            g2.dispose();
        }
        return image;
    }

    private static void assertImagesEqual(BufferedImage expected, BufferedImage actual) {
        assertEquals(expected.getWidth(), actual.getWidth());
        assertEquals(expected.getHeight(), actual.getHeight());
        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                assertEquals(expected.getRGB(x, y), actual.getRGB(x, y),
                        "pixel mismatch at " + x + "," + y);
            }
        }
    }

    private static void onEdt(Runnable action) throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(action);
    }
}
