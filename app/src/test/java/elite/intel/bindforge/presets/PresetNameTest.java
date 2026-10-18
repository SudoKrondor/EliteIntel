package elite.intel.bindforge.presets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The name a file name implies - the table in preset-editor.md, "What a line actually holds". */
class PresetNameTest {

    @Test
    void versionSuffixIsStripped() {
        assertEquals("DualVirpilDawnTreader", PresetName.fromFileName("DualVirpilDawnTreader.4.2.binds"));
        assertEquals("Custom", PresetName.fromFileName("Custom.4.2.binds"));
        assertEquals("Custom", PresetName.fromFileName("Custom.3.0.binds"));
    }

    @Test
    void aShippedPresetHasNoSuffixToStrip() {
        assertEquals("KeyboardMouseOnly", PresetName.fromFileName("KeyboardMouseOnly.binds"));
    }

    @Test
    void aNameThatDoesNotEndInASuffixKeepsItsWholeStem() {
        assertEquals("Custom.4.2 (2)", PresetName.fromFileName("Custom.4.2 (2).binds"));
        assertEquals("Custom.4", PresetName.fromFileName("Custom.4.binds"));
    }

    @Test
    void onlyTheLastSuffixIsStripped() {
        assertEquals("Ship.1.0", PresetName.fromFileName("Ship.1.0.4.2.binds"));
    }

    @Test
    void aNameWithoutTheExtensionIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> PresetName.fromFileName("Custom.4.2.binds.backup"));
    }
}
