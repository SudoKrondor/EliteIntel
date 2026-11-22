package elite.intel.bindforge.storage;

import elite.intel.bindforge.install.GameInstallation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static elite.intel.bindforge.storage.WorkingCopyTest.text;
import static elite.intel.bindforge.storage.WorkingCopyTest.write;
import static org.junit.jupiter.api.Assertions.*;

/** An installation's device files are found ignoring case, as the game finds them (Alan, 2026-11-07, Storage S13a D5). */
class DeviceFilesTest {

    @TempDir
    Path root;

    @Test
    void aDeviceMappingsInAnotherCaseIsRead() throws IOException {
        Path controlSchemes = GameInstallation.controlSchemesUnder(root);
        Files.createDirectories(controlSchemes);
        write(controlSchemes.resolve("devicemappings.xml"), "<Mine/>");

        DeviceFiles files = DeviceFiles.read(root);

        assertTrue(files.hasDeviceMappings());
        assertEquals("<Mine/>", text(files.deviceMappings()));
    }

    @Test
    void aButtonMapIsFoundWhateverItsCase() throws IOException {
        Path maps = GameInstallation.deviceButtonMapsIn(GameInstallation.controlSchemesUnder(root));
        Files.createDirectories(maps);
        write(maps.resolve("lvwap.buttonMap"), "<Root/>");

        DeviceFiles files = DeviceFiles.read(root);

        assertFalse(files.hasDeviceMappings());
        assertTrue(files.buttonMaps().containsKey("LVWAP.buttonMap"));
    }

    /**
     * Two spellings of one map handed in are refused, as {@link ConfigurationFiles} refuses them - never one silently
     * kept (Storage S13aR, D2).
     */
    @Test
    void twoSpellingsOfOneMapAreRefused() {
        Map<String, byte[]> both = Map.of("LVWAP.buttonMap", new byte[0], "lvwap.buttonMap", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> new DeviceFiles(null, both));
        assertThrows(IllegalArgumentException.class, () -> new InstallationFiles(1, "STEAM", root, null, both));
    }
}
