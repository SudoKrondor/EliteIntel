package elite.intel.ui.theme.preview;

import elite.intel.i18n.Language;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThemePreviewRequestTest {

    @Test
    void aRequestSurvivesTheCommandLine() {
        ThemePreviewRequest request = new ThemePreviewRequest(ThemePreviewRequest.Mode.ROLE_MAP, Language.RU,
                ThemePreviewRequest.TextAntialiasing.LCD_HRGB, 140, 1.5);

        assertEquals(request, ThemePreviewRequest.fromArguments(request.toArguments().toArray(String[]::new)));
    }

    @Test
    void theAppsTextHintIsFoundByItsSwingValue() {
        assertEquals(ThemePreviewRequest.TextAntialiasing.LCD_VBGR,
                ThemePreviewRequest.TextAntialiasing.of(RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VBGR));
        assertEquals(ThemePreviewRequest.TextAntialiasing.DEFAULT, ThemePreviewRequest.TextAntialiasing.of(null));
    }
}
