package elite.intel.bindforge.model;

import elite.intel.bindforge.io.BindsValues;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Toggles tab's list, its reading of a value and what it writes (bind-editor.md, <em>Toggles</em>).
 */
class SettingTogglesTest {

    /**
     * WHY not shown on any options screen: they stay under OTHER, by their XML names, until someone finds their rows.
     */
    private static final Set<String> UNNAMED_IN_GAME = Set.of("MotionHeadlook", "yawRotateHeadlook", "EnableRumbleTrigger");

    @Test
    void theBooleansListButThePartsOfABinding() {
        assertEquals(42, SettingToggles.SETTINGS.size(), "45 booleans less Hold, Inverted and ToggleOn");
        assertFalse(SettingToggles.SETTINGS.contains("Hold"));
        assertFalse(SettingToggles.SETTINGS.contains("Inverted"));
        assertFalse(SettingToggles.SETTINGS.contains("ToggleOn"));
    }

    @Test
    void noSettingIsBothAToggleAndANumber() {
        Set<String> both = new TreeSet<>(SettingToggles.SETTINGS);
        both.retainAll(SettingNumbers.SETTINGS);
        assertTrue(both.isEmpty(), both.toString());
    }

    @Test
    void onlyOneIsOn() {
        assertTrue(SettingToggles.on("1"));
        assertFalse(SettingToggles.on("0"));
        assertFalse(SettingToggles.on(""));
        assertFalse(SettingToggles.on("yes"));
        assertFalse(SettingToggles.on("1.0"));
    }

    @Test
    void writesWhatTheGameWrites() {
        assertEquals("1", SettingToggles.written(true));
        assertEquals("0", SettingToggles.written(false));
    }

    /**
     * Every toggle a game-saved 4.2 file holds has its in-game name, but the three no options screen shows.
     */
    @Test
    void everyToggleInARealFileIsNamedAsTheGameNamesIt() throws IOException {
        BindsValues values = BindsValues.of(Files.readAllBytes(
                Path.of("../docs/02-features/bindforge/reference-data/Custom.4.2.binds")));

        SettingToggles.SETTINGS.stream()
                .filter(values.settings()::containsKey)
                .filter(id -> !UNNAMED_IN_GAME.contains(id))
                .forEach(id -> assertNotEquals(BindingSection.OTHER, BindingDisplayNames.lookup(id).section(), id));
    }

    @Test
    void showMouseWidgetIsInMouseControls() {
        BindingDisplayNames.ControlName control = BindingDisplayNames.lookup("MouseGUI");
        assertEquals(BindingSection.SHIP, control.section());
        assertEquals("Mouse Controls", control.group());
        assertEquals("Show Mouse Widget", control.name());
    }
}
