package elite.intel.ui.theme;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ThemeLibraryTest {

    @TempDir
    Path folder;

    @Test
    void theBuiltInThemesComeFirstThenTheCommandersThemesByName() throws IOException {
        ThemeLibrary library = new ThemeLibrary(folder);
        library.create(theme("Zulu"));
        library.create(theme("alpha"));

        List<String> names = library.entries().stream().map(entry -> entry.theme().name()).toList();

        assertEquals(List.of("Default", "High contrast", "Clockwork Orange", "alpha", "Zulu"), names);
        assertTrue(library.entries().get(2).builtIn());
    }

    @Test
    void aTakenNameGetsANumber() throws IOException {
        ThemeLibrary library = new ThemeLibrary(folder);
        library.create(theme("Night"));

        ThemeLibrary.Entry second = library.create(theme("night"));

        assertEquals("night 2", second.theme().name());
        assertNotEquals("night.json", second.id());
    }

    /** The id is what the active-theme setting stores: a rename must not break it. */
    @Test
    void aRenameKeepsTheThemesFileAndId() throws IOException, ThemeFileException {
        ThemeLibrary library = new ThemeLibrary(folder);
        ThemeLibrary.Entry entry = library.create(theme("Night"));

        ThemeLibrary.Entry renamed = library.rename(entry, "Night Ops");

        assertEquals(entry.id(), renamed.id());
        assertEquals("Night Ops", ThemeFile.read(folder.resolve(entry.id())).name());
    }

    @Test
    void aNameWithNoLatinLettersStillGetsAFile() throws IOException {
        ThemeLibrary library = new ThemeLibrary(folder);

        ThemeLibrary.Entry entry = library.create(theme("Ночная"));

        assertEquals("theme.json", entry.id());
        assertEquals("Ночная", entry.theme().name());
    }

    @Test
    void anUnreadableFileIsLeftOutAndTheRestListed() throws IOException {
        ThemeLibrary library = new ThemeLibrary(folder);
        library.create(theme("Good"));
        Files.writeString(folder.resolve("broken.json"), "not a theme");

        assertEquals(List.of("Default", "High contrast", "Clockwork Orange", "Good"), library.entries().stream().map(entry -> entry.theme().name()).toList());
    }

    @Test
    void aBuiltInThemeCannotBeChanged() throws IOException {
        ThemeLibrary library = new ThemeLibrary(folder);
        ThemeLibrary.Entry builtIn = library.entries().getFirst();

        assertThrows(IllegalArgumentException.class, () -> library.save(builtIn, theme("Hacked")));
        assertThrows(IllegalArgumentException.class, () -> library.delete(builtIn));
    }

    private static ColorTheme theme(String name) {
        return new ColorTheme(name, "", Map.of(ThemeRole.DANGER, new Color(0xFF3355)));
    }
}
