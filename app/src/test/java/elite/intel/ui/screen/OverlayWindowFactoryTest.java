package elite.intel.ui.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayWindowFactoryTest {

    @Test
    void selectsTheLegacyPresentationOnlyForLinux() {
        assertTrue(OverlayWindowFactory.usesLegacyLinuxOverlay("Linux"));
        assertTrue(OverlayWindowFactory.usesLegacyLinuxOverlay("GNU/Linux"));
        assertFalse(OverlayWindowFactory.usesLegacyLinuxOverlay("Windows 11"));
        assertFalse(OverlayWindowFactory.usesLegacyLinuxOverlay("Mac OS X"));
        assertFalse(OverlayWindowFactory.usesLegacyLinuxOverlay(null));
    }
}
