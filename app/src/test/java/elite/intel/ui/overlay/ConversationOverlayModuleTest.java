package elite.intel.ui.overlay;

import elite.intel.ui.theme.HudPalette;
import elite.intel.ui.widget.HudLogArea;
import elite.intel.ui.widget.HudSection;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConversationOverlayModuleTest {

    @Test
    void overlayConversationHasNoHeaderAndKeepsItsViewportTransparent() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ConversationOverlayModule module = new ConversationOverlayModule();
            HudSection section = (HudSection) module.component();
            BorderLayout layout = (BorderLayout) section.getLayout();

            assertNull(layout.getLayoutComponent(section, BorderLayout.NORTH));
            assertFalse(section.body().isOpaque());
            assertEquals(179, HudPalette.HUD_COLOR_ROLE_OVERLAY_CARD_BACKGROUND.getAlpha());
            assertEquals(540, HudPalette.HUD_OVERLAY_DEFAULT_WIDTH);

            JScrollPane scrollPane = (JScrollPane) section.body().getComponent(0);
            assertFalse(scrollPane.isOpaque());
            assertFalse(scrollPane.getViewport().isOpaque());
        });
    }

    @Test
    void overlayChatLeavesNoIdlePromptOnItsTransparentCanvas() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            HudLogArea overlay = HudLogArea.overlayChat(25);
            overlay.setSize(160, 60);

            BufferedImage canvas = new BufferedImage(160, 60, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = canvas.createGraphics();
            try {
                graphics.setComposite(AlphaComposite.Clear);
                graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                graphics.setComposite(AlphaComposite.SrcOver);
                overlay.paint(graphics);
            } finally {
                graphics.dispose();
            }

            for (int y = 0; y < canvas.getHeight(); y++) {
                for (int x = 0; x < canvas.getWidth(); x++) {
                    assertEquals(0, (canvas.getRGB(x, y) >>> 24) & 0xFF);
                }
            }
        });
    }
}
