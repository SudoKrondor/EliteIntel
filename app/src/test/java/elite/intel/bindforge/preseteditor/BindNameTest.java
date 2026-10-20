package elite.intel.bindforge.preseteditor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The BindName a file name implies - the table in preset-editor.md, "What a line actually holds". */
class BindNameTest {

    @Test
    void versionSuffixIsStripped() {
        assertEquals("DualVirpilDawnTreader", BindName.fromFileName("DualVirpilDawnTreader.4.2.binds"));
        assertEquals("Custom", BindName.fromFileName("Custom.4.2.binds"));
        assertEquals("Custom", BindName.fromFileName("Custom.3.0.binds"));
    }

    @Test
    void aFactoryBindsFileHasNoSuffixToStrip() {
        assertEquals("KeyboardMouseOnly", BindName.fromFileName("KeyboardMouseOnly.binds"));
    }

    @Test
    void aNameThatDoesNotEndInASuffixKeepsItsWholeStem() {
        assertEquals("Custom.4.2 (2)", BindName.fromFileName("Custom.4.2 (2).binds"));
        assertEquals("Custom.4", BindName.fromFileName("Custom.4.binds"));
    }

    @Test
    void onlyTheLastSuffixIsStripped() {
        assertEquals("Ship.1.0", BindName.fromFileName("Ship.1.0.4.2.binds"));
    }

    @Test
    void aNameWithoutTheExtensionIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> BindName.fromFileName("Custom.4.2.binds.backup"));
    }
}
