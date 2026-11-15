package elite.intel.ui.theme;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ThemeFileTest {

    @TempDir
    Path folder;

    @Test
    void aThemeSurvivesWritingAndReadingBack() throws IOException, ThemeFileException {
        ColorTheme theme = new ColorTheme("Night Ops", "CMDR Example", Map.of(
                ThemeRole.SUCCESS, new Color(0x3B9EFF),
                ThemeRole.PRIMARY_ACTION, new Color(0x80FFAA00, true)));
        Path file = folder.resolve("night-ops.json");

        ThemeFile.write(file, theme);

        assertEquals(theme, ThemeFile.read(file));
    }

    @Test
    void onlyColoursThatDifferFromDefaultAreWritten() throws ThemeFileException {
        ColorTheme theme = new ColorTheme("Mostly default", "", Map.of(
                ThemeRole.SUCCESS, ThemeRole.SUCCESS.defaultColor(),
                ThemeRole.DANGER, new Color(0xFF0000)));

        String json = ThemeFile.format(theme);

        assertFalse(json.contains("\"success\""), json);
        assertTrue(json.contains("\"danger\": \"#FF0000\""), json);
        assertEquals(Map.of(ThemeRole.DANGER, new Color(0xFF0000)), ThemeFile.parse(json).overrides());
    }

    @Test
    void colourWithAlphaIsWrittenWithItsAlpha() {
        assertEquals("#A633D7E8", ThemeFile.hex(new Color(0xA633D7E8, true)));
        assertEquals("#7CE8A8", ThemeFile.hex(new Color(0x7CE8A8)));
    }

    /**
     * A theme from a newer version must still load here: what this build does not understand is dropped, the rest
     * of the theme is kept.
     */
    @Test
    void unknownRoleAndUnreadableColourAreDroppedAndTheRestLoads() throws ThemeFileException {
        ColorTheme theme = ThemeFile.parse("""
                { "format": 1, "name": "From the future",
                  "colors": { "hologramGlow": "#123456", "danger": "red", "warning": 42, "success": "#3B9EFF" } }
                """);

        assertEquals(Map.of(ThemeRole.SUCCESS, new Color(0x3B9EFF)), theme.overrides());
    }

    @Test
    void roleTheFileLeavesOutTakesItsDefault() throws ThemeFileException {
        ColorTheme theme = ThemeFile.parse("{ \"format\": 1, \"name\": \"Empty\" }");

        assertEquals(ThemeRole.FRAME_BORDER.defaultColor(), theme.color(ThemeRole.FRAME_BORDER));
    }

    /** A theme made before the accent fill existed keeps its own accent there, not the Default's cyan. */
    @Test
    void anAccentFillTheFileLeavesOutShowsItsAccent() throws ThemeFileException {
        ColorTheme theme = ThemeFile.parse("""
                { "format": 1, "name": "Orange", "colors": { "primaryAction": "#FF6600" } }
                """);

        assertEquals(new Color(0xFF6600), theme.color(ThemeRole.ACCENT_FILL));
    }

    @Test
    void anAccentFillEqualToItsAccentIsNotWrittenButAnyOtherIs() throws ThemeFileException {
        Color orange = new Color(0xFF6600);
        ColorTheme following = new ColorTheme("Orange", "", Map.of(
                ThemeRole.PRIMARY_ACTION, orange, ThemeRole.ACCENT_FILL, orange));
        ColorTheme cyanFill = new ColorTheme("Orange, cyan selections", "", Map.of(
                ThemeRole.PRIMARY_ACTION, orange, ThemeRole.ACCENT_FILL, ThemeRole.ACCENT_FILL.defaultColor()));

        assertFalse(ThemeFile.format(following).contains("accentFill"));
        assertEquals(ThemeRole.ACCENT_FILL.defaultColor(), ThemeFile.parse(ThemeFile.format(cyanFill)).color(ThemeRole.ACCENT_FILL));
    }

    @Test
    void aNewerFormatIsRefused() {
        ThemeFileException refused = assertThrows(ThemeFileException.class,
                () -> ThemeFile.parse("{ \"format\": 2, \"name\": \"Later\", \"colors\": {} }"));

        assertTrue(refused.getMessage().contains("newer"), refused.getMessage());
    }

    @Test
    void whatIsNotAThemeIsRefused() {
        assertThrows(ThemeFileException.class, () -> ThemeFile.parse("not json at all {"));
        assertThrows(ThemeFileException.class, () -> ThemeFile.parse("[1, 2, 3]"));
        assertThrows(ThemeFileException.class, () -> ThemeFile.parse("{ \"name\": \"No format\" }"));
        assertThrows(ThemeFileException.class, () -> ThemeFile.parse("{ \"format\": 1, \"name\": \"  \" }"));
    }

    @Test
    void anOversizedFileIsRefusedWithoutReadingIt() throws IOException {
        Path file = folder.resolve("huge.json");
        Files.write(file, new byte[(int) ThemeFile.MAX_BYTES + 1]);

        assertThrows(ThemeFileException.class, () -> ThemeFile.read(file));
    }
}
