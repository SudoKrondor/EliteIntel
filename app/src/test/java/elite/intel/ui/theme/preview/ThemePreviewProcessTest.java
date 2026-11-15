package elite.intel.ui.theme.preview;

import elite.intel.i18n.Language;
import elite.intel.ui.theme.ColorTheme;
import elite.intel.ui.theme.ThemeRole;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the real preview process: the draft theme must reach the picture, and a click on what it painted must find
 * the role it was painted for.
 */
class ThemePreviewProcessTest {

    private static final int DRAFT_DANGER = 0x123456;

    @Test
    void aDraftColourIsDrawnAndTheRoleMapFindsItsRole() throws IOException {
        ThemePreviewRequest request = new ThemePreviewRequest(ThemePreviewRequest.Mode.PREVIEW, Language.EN,
                ThemePreviewRequest.TextAntialiasing.ON, 0, 1.0);
        ColorTheme draft = new ColorTheme("Draft", "", Map.of(ThemeRole.DANGER, new Color(DRAFT_DANGER)));

        BufferedImage preview = ThemePreviewProcess.preview(request, draft);
        RoleMap roleMap = ThemePreviewProcess.roleMap(request);

        int painted = 0;
        int foundAsDanger = 0;
        for (int y = 0; y < preview.getHeight(); y++) {
            for (int x = 0; x < preview.getWidth(); x++) {
                if ((preview.getRGB(x, y) & 0xFFFFFF) != DRAFT_DANGER) continue;
                painted++;
                if (roleMap.roleAt(x, y).equals(Optional.of(ThemeRole.DANGER))) foundAsDanger++;
            }
        }
        assertTrue(painted > 0, "the draft danger colour appears nowhere in the preview");
        // WHY most, not all: the preview's text is anti-aliased and the role map's is not, so a glyph's edge can
        // fall a pixel apart between the two.
        assertTrue(foundAsDanger * 2 > painted, foundAsDanger + " of " + painted + " danger pixels map to DANGER");
    }

    /**
     * A swatch whose parts the preview cannot show names them instead ({@code themes.part.<wireName>}): a part the
     * preview stops drawing, after a change to the sample, must get a name or its swatch would show a bare key.
     */
    @Test
    void everyPartThePreviewCannotShowHasAName() throws IOException {
        ThemePreviewRequest request = new ThemePreviewRequest(ThemePreviewRequest.Mode.PREVIEW, Language.EN,
                ThemePreviewRequest.TextAntialiasing.ON, 0, 1.0);
        RoleMap roleMap = ThemePreviewProcess.roleMap(request);
        ResourceBundle english = ResourceBundle.getBundle("i18n.gui", java.util.Locale.ROOT);

        List<String> unnamed = Arrays.stream(ThemeRole.values())
                .filter(role -> !roleMap.paints(role))
                .map(role -> "themes.part." + role.wireName())
                .filter(key -> !english.containsKey(key))
                .toList();

        assertEquals(List.of(), unnamed);
    }
}
