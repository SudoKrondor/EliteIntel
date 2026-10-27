package elite.intel.bindforge.storage;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static elite.intel.bindforge.storage.WorkingCopyTest.bytes;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Whether an installation's device files hold what the working copy holds, by value (Alan, 2026-10-26): APPLY leaves
 * one that differs alone, for the check to reconcile.
 */
class DeviceFilesComparisonTest {

    private static final String LVWAP = "<LVWAP><PID>83F4</PID><VID>3344</VID></LVWAP>";
    private static final String RUDDER = "<T-Rudder><PID>B679</PID><VID>044F</VID></T-Rudder>";

    @Test
    void theSameValuesMatchWhateverTheBytes() {
        ConfigurationFiles workingCopy = workingCopy("<Root>" + LVWAP + RUDDER + "</Root>",
                Map.of("LVWAP.buttonMap", "<Root><Joy_1>FIRE</Joy_1></Root>"));
        InstallationFiles live = live("<?xml version=\"1.0\"?>\r\n<Root>\r\n  " + LVWAP + "\r\n  " + RUDDER
                + "\r\n</Root>\r\n", Map.of("lvwap.buttonMap", "<Root>\r\n  <Joy_1> FIRE </Joy_1>\r\n</Root>"));

        assertFalse(DeviceFilesComparison.differs(workingCopy, live), "line endings, spacing and case are not values");
    }

    @Test
    void anEntryTheWorkingCopyLacksDiffers() {
        assertTrue(DeviceFilesComparison.differs(workingCopy("<Root>" + RUDDER + "</Root>", Map.of()),
                live("<Root>" + LVWAP + RUDDER + "</Root>", Map.of())));
    }

    /** The first element claiming a VID/PID is the one the game uses, so the order is a value. */
    @Test
    void theSameEntriesInAnotherOrderDiffer() {
        assertTrue(DeviceFilesComparison.differs(workingCopy("<Root>" + LVWAP + RUDDER + "</Root>", Map.of()),
                live("<Root>" + RUDDER + LVWAP + "</Root>", Map.of())));
    }

    @Test
    void anotherLabelDiffers() {
        assertTrue(DeviceFilesComparison.differs(
                workingCopy("<Root>" + LVWAP + "</Root>", Map.of("LVWAP.buttonMap", "<Root><Joy_1>FIRE</Joy_1></Root>")),
                live("<Root>" + LVWAP + "</Root>", Map.of("LVWAP.buttonMap", "<Root><Joy_1>PINKY</Joy_1></Root>"))));
    }

    /** A map added by hand inside an installation is the user's, and never removed unasked. */
    @Test
    void aMapTheWorkingCopyLacksDiffers() {
        assertTrue(DeviceFilesComparison.differs(workingCopy("<Root>" + LVWAP + "</Root>", Map.of()),
                live("<Root>" + LVWAP + "</Root>", Map.of("Spare.buttonMap", "<Root/>"))));
    }

    /** Frontier's own VPCPanel map is never removed by a write, so the working copy lacking it is no difference. */
    @Test
    void frontiersShippedMapIsNoDifference() {
        assertFalse(DeviceFilesComparison.differs(workingCopy("<Root>" + LVWAP + "</Root>", Map.of()),
                live("<Root>" + LVWAP + "</Root>", Map.of("VPCPanel.buttonMap", "<Root/>"))));
    }

    /** With no DeviceMappings.xml there is nothing of the user's in it, and APPLY simply writes one. */
    @Test
    void anInstallationWithNoDeviceMappingsDoesNotDiffer() {
        assertFalse(DeviceFilesComparison.differs(workingCopy("<Root>" + LVWAP + "</Root>", Map.of()),
                live(null, Map.of())));
    }

    /** What an unreadable file holds is unknown, so it is never written over unseen. */
    @Test
    void aFileThatCannotBeReadDiffers() {
        assertTrue(DeviceFilesComparison.differs(workingCopy("<Root>" + LVWAP + "</Root>", Map.of()),
                live("not xml at all", Map.of())));
    }

    private static ConfigurationFiles workingCopy(String deviceMappings, Map<String, String> maps) {
        return new ConfigurationFiles(Map.of(), bytes(deviceMappings), bytesOf(maps));
    }

    private static InstallationFiles live(String deviceMappings, Map<String, String> maps) {
        return new InstallationFiles(1, "STEAM", Path.of("Steam"), deviceMappings == null ? null : bytes(deviceMappings),
                bytesOf(maps));
    }

    private static Map<String, byte[]> bytesOf(Map<String, String> maps) {
        Map<String, byte[]> result = new java.util.TreeMap<>();
        maps.forEach((name, text) -> result.put(name, bytes(text)));
        return result;
    }
}
