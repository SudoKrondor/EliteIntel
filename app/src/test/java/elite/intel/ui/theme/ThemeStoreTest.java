package elite.intel.ui.theme;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ThemeStoreTest {

    @TempDir
    Path folder;

    @Test
    void emptyIdIsTheDefaultTheme() throws IOException, ThemeFileException {
        assertSame(ColorTheme.DEFAULT, store().load(ThemeStore.DEFAULT_ID));
    }

    @Test
    void anIdNamesAFileInTheThemesFolder() throws IOException, ThemeFileException {
        ColorTheme theme = new ColorTheme("Night Ops", "", Map.of(ThemeRole.DANGER, new Color(0xFF3355)));
        ThemeFile.write(folder.resolve("night-ops.json"), theme);

        assertEquals(theme, store().load("night-ops.json"));
    }

    /** The app must start whatever happened to the chosen theme. */
    @Test
    void aMissingOrBrokenThemeFallsBackToDefault() throws IOException {
        Files.writeString(folder.resolve("broken.json"), "{ \"format\": 1 ");

        assertSame(ColorTheme.DEFAULT, store().loadOrDefault("gone.json"));
        assertSame(ColorTheme.DEFAULT, store().loadOrDefault("broken.json"));
        assertSame(ColorTheme.DEFAULT, store().loadOrDefault(ThemeStore.BUILT_IN_PREFIX + "no-such-theme"));
    }

    @Test
    void anIdCannotReachOutsideTheThemesFolder() throws IOException {
        Path outside = folder.resolveSibling(folder.getFileName() + "-outside.json");
        ThemeFile.write(outside, new ColorTheme("Elsewhere", "", Map.of()));
        try {
            assertThrows(ThemeFileException.class, () -> store().load("../" + outside.getFileName()));
            assertThrows(ThemeFileException.class, () -> store().load(outside.toString()));
        } finally {
            Files.deleteIfExists(outside);
        }
    }

    private ThemeStore store() {
        return new ThemeStore(folder);
    }
}
