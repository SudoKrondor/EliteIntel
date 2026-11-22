package elite.intel.bindforge.storage;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static elite.intel.bindforge.storage.WorkingCopyTest.bytes;
import static org.junit.jupiter.api.Assertions.*;

/** One set holds one spelling of each file, and finds it whatever the case (Krondor and Alan, 2026-11-07, Storage S13a). */
class ConfigurationFilesTest {

    @Test
    void twoSpellingsOfOneFileAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> new ConfigurationFiles(
                Map.of("Custom.4.2.binds", bytes("<a/>"), "custom.4.2.binds", bytes("<b/>")), null, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new ConfigurationFiles(Map.of(), null,
                Map.of("LVWAP.buttonMap", bytes("<a/>"), "lvwap.buttonMap", bytes("<b/>"))));
    }

    @Test
    void aFileIsFoundWhateverItsCaseAndHeldUnderItsOwnSpelling() {
        ConfigurationFiles set = new ConfigurationFiles(Map.of("Custom.4.2.binds", bytes("<Custom/>")), null,
                Map.of("LVWAP.buttonMap", bytes("<LVWAP/>")));

        assertArrayEquals(bytes("<Custom/>"), set.bindings().get("custom.4.2.binds"));
        assertTrue(set.buttonMaps().containsKey("lvwap.buttonMap"));
        assertEquals(List.of("Custom.4.2.binds"), List.copyOf(set.bindings().keySet()));
    }

    /** A file put in under another spelling replaces the one there, and the spelling given is the one kept. */
    @Test
    void aFileInAnotherCaseReplacesTheOneThereUnderTheNewSpelling() {
        ConfigurationFiles set = new ConfigurationFiles(Map.of("Custom.4.2.binds", bytes("<Custom/>")), null, Map.of());

        ConfigurationFiles changed = set.withBindingsFile("custom.4.2.binds", bytes("<custom/>"));

        assertEquals(List.of("custom.4.2.binds"), List.copyOf(changed.bindings().keySet()));
        assertArrayEquals(bytes("<custom/>"), changed.bindings().get("custom.4.2.binds"));
    }

    /** The edits are told from the working copy by spelling too: a respelled file is a change. */
    @Test
    void aRespelledFileIsNotTheSameSet() {
        ConfigurationFiles set = new ConfigurationFiles(Map.of("Custom.4.2.binds", bytes("<Custom/>")), null, Map.of());

        assertFalse(set.sameAs(set.withBindingsFile("custom.4.2.binds", bytes("<Custom/>"))));
        assertTrue(set.sameAs(set.withBindingsFile("Custom.4.2.binds", bytes("<Custom/>"))));
    }
}
