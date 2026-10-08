package elite.intel.bindforge.install;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The device-file paths, spelled once: and a device name never steers one out of its folder. */
class GameInstallationTest {

    @TempDir
    Path controlSchemes;

    @Test
    void aButtonMapSitsInDeviceButtonMapsUnderTheDevicesName() {
        assertEquals(controlSchemes.resolve("DeviceButtonMaps").resolve("T-Rudder.buttonMap"),
                GameInstallation.buttonMapIn(controlSchemes, "T-Rudder"));
        assertEquals(controlSchemes.resolve("DeviceMappings.xml"), GameInstallation.deviceMappingsIn(controlSchemes));
    }

    /**
     * An XML tag may hold a colon, and on Windows "C:x" is a path on drive C rather than a filename - resolving it
     * would leave the game folder. Found 2026-10-07 when a test of this case tried to write to drive A.
     */
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void aDriveLetterInTheNameIsRefused() {
        assertThrows(InvalidPathException.class, () -> GameInstallation.buttonMapIn(controlSchemes, "C:Evil"));
        assertThrows(InvalidPathException.class, () -> GameInstallation.buttonMapIn(controlSchemes, "a:b"));
    }
}
