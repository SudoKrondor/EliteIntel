package elite.intel.bindforge.storage;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

import static elite.intel.bindforge.storage.WorkingCopyTest.bytes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Whether an installation's device files hold what the working copy holds, by value (Alan, 2026-10-26): APPLY leaves
 * one that differs alone, for the check to reconcile - and how it differs, which the check names (Storage S9a).
 */
class DeviceFilesComparisonTest {

    private static final String LVWAP = "<LVWAP><PID>83F4</PID><VID>3344</VID></LVWAP>";
    private static final String RUDDER = "<T-Rudder><PID>B679</PID><VID>044F</VID></T-Rudder>";

    @Test
    void theSameValuesMatchWhateverTheBytes() throws IOException {
        ConfigurationFiles workingCopy = workingCopy("<Root>" + LVWAP + RUDDER + "</Root>",
                Map.of("LVWAP.buttonMap", "<Root><Joy_1>FIRE</Joy_1></Root>"));
        DeviceFiles live = live("<?xml version=\"1.0\"?>\r\n<Root>\r\n  " + LVWAP + "\r\n  " + RUDDER
                + "\r\n</Root>\r\n", Map.of("lvwap.buttonMap", "<Root>\r\n  <Joy_1> FIRE </Joy_1>\r\n</Root>"));

        assertFalse(differs(workingCopy, live), "line endings, spacing and case are not values");
    }

    @Test
    void anEntryTheWorkingCopyLacksDiffers() throws IOException {
        assertTrue(differs(workingCopy("<Root>" + RUDDER + "</Root>", Map.of()),
                live("<Root>" + LVWAP + RUDDER + "</Root>", Map.of())));
    }

    /** The first element claiming a VID/PID is the one the game uses, so the order is a value. */
    @Test
    void theSameEntriesInAnotherOrderDiffer() throws IOException {
        assertTrue(differs(workingCopy("<Root>" + LVWAP + RUDDER + "</Root>", Map.of()),
                live("<Root>" + RUDDER + LVWAP + "</Root>", Map.of())));
    }

    @Test
    void anotherLabelDiffers() throws IOException {
        assertTrue(differs(
                workingCopy("<Root>" + LVWAP + "</Root>", Map.of("LVWAP.buttonMap", "<Root><Joy_1>FIRE</Joy_1></Root>")),
                live("<Root>" + LVWAP + "</Root>", Map.of("LVWAP.buttonMap", "<Root><Joy_1>PINKY</Joy_1></Root>"))));
    }

    /** A map added by hand inside an installation is the user's, and never removed unasked. */
    @Test
    void aMapTheWorkingCopyLacksDiffers() throws IOException {
        assertTrue(differs(workingCopy("<Root>" + LVWAP + "</Root>", Map.of()),
                live("<Root>" + LVWAP + "</Root>", Map.of("Spare.buttonMap", "<Root/>"))));
    }

    /** Frontier's own VPCPanel map is never removed by a write, so the working copy lacking it is no difference. */
    @Test
    void frontiersShippedMapIsNoDifference() throws IOException {
        assertFalse(differs(workingCopy("<Root>" + LVWAP + "</Root>", Map.of()),
                live("<Root>" + LVWAP + "</Root>", Map.of("VPCPanel.buttonMap", "<Root/>"))));
    }

    /** With no DeviceMappings.xml there is nothing of the user's in it, and APPLY simply writes one. */
    @Test
    void anInstallationWithNoDeviceMappingsDoesNotDiffer() throws IOException {
        assertFalse(differs(workingCopy("<Root>" + LVWAP + "</Root>", Map.of()),
                live(null, Map.of())));
    }

    /** An element's children, such as SupportsIcons, are Frontier's and not compared (Alan, 2026-10-27). */
    @Test
    void anElementsChildrenAreNotCompared() throws IOException {
        String withIcons = "<LVWAP><PID>83F4</PID><VID>3344</VID><SupportsIcons>1</SupportsIcons></LVWAP>";

        assertFalse(differs(workingCopy("<Root>" + LVWAP + "</Root>", Map.of()),
                live("<Root>" + withIcons + "</Root>", Map.of())));
    }

    /** An installation file that cannot be parsed is thrown, for APPLY to log and leave that installation alone. */
    @Test
    void anInstallationFileThatCannotBeParsedIsThrown() throws IOException {
        DeviceFilesComparison comparison = DeviceFilesComparison.of(workingCopy("<Root>" + LVWAP + "</Root>", Map.of()));

        assertThrows(IOException.class, () -> comparison.differs(live("not xml at all", Map.of())));
    }

    /** The working copy is BindForge's own: one that cannot be parsed is thrown before any installation is read. */
    @Test
    void aWorkingCopyThatCannotBeParsedIsThrown() {
        assertThrows(IOException.class, () -> DeviceFilesComparison.of(workingCopy("not xml at all", Map.of())));
    }

    // ---- the check's verdict (Storage S9a)

    /** Frontier ships T-Rudder in these tests; LVWAP is the user's. */
    private static final Predicate<String> SHIPS = "T-Rudder"::equals;

    @Test
    void theSameValuesAreAMatch() throws IOException {
        assertEquals(DeviceFilesComparison.Verdict.MATCHES, judge(workingCopy("<Root>" + LVWAP + RUDDER + "</Root>",
                Map.of()), live("<Root>\r\n" + LVWAP + RUDDER + "</Root>\r\n", Map.of())));
    }

    @Test
    void anotherEntryOfTheUsersDiffers() throws IOException {
        String lvwapMoved = "<LVWAP><PID>83F5</PID><VID>3344</VID></LVWAP>";

        assertEquals(DeviceFilesComparison.Verdict.DIFFERS, judge(workingCopy("<Root>" + LVWAP + RUDDER + "</Root>",
                Map.of()), live("<Root>" + lvwapMoved + RUDDER + "</Root>", Map.of())));
    }

    /** Unlike APPLY's test, a missing DeviceMappings.xml is a difference at the check: a reset (Alan, 2026-10-31). */
    @Test
    void aMissingDeviceMappingsIsAReset() throws IOException {
        DeviceFilesComparison.Verdict verdict = judge(workingCopy("<Root>" + LVWAP + "</Root>", Map.of()),
                live(null, Map.of()));

        assertEquals(DeviceFilesComparison.Verdict.RESET_MISSING, verdict);
        assertTrue(verdict.reset());
    }

    /** None of the user's elements left, while the working copy holds one: a factory file, a reset. */
    @Test
    void aFileHoldingOnlyFrontiersEntriesIsAReset() throws IOException {
        assertEquals(DeviceFilesComparison.Verdict.RESET_FACTORY, judge(
                workingCopy("<Root>" + LVWAP + RUDDER + "</Root>", Map.of()), live("<Root>" + RUDDER + "</Root>",
                        Map.of())));
    }

    /** A working copy that holds nothing of the user's cannot be reset back to Frontier's: it is a difference. */
    @Test
    void frontiersEntriesAloneOnBothSidesAreNoReset() throws IOException {
        String rudderMoved = "<T-Rudder><PID>B67A</PID><VID>044F</VID></T-Rudder>";

        assertEquals(DeviceFilesComparison.Verdict.DIFFERS, judge(workingCopy("<Root>" + RUDDER + "</Root>",
                Map.of()), live("<Root>" + rudderMoved + "</Root>", Map.of())));
    }

    private static DeviceFilesComparison.Verdict judge(ConfigurationFiles workingCopy, DeviceFiles live)
            throws IOException {
        return DeviceFilesComparison.of(workingCopy, SHIPS).judge(live);
    }

    private static boolean differs(ConfigurationFiles workingCopy, DeviceFiles live) throws IOException {
        return DeviceFilesComparison.of(workingCopy).differs(live);
    }

    private static ConfigurationFiles workingCopy(String deviceMappings, Map<String, String> maps) {
        return new ConfigurationFiles(Map.of(), bytes(deviceMappings), bytesOf(maps));
    }

    private static DeviceFiles live(String deviceMappings, Map<String, String> maps) {
        return new DeviceFiles(deviceMappings == null ? null : bytes(deviceMappings), bytesOf(maps));
    }

    private static Map<String, byte[]> bytesOf(Map<String, String> maps) {
        Map<String, byte[]> result = new TreeMap<>();
        maps.forEach((name, text) -> result.put(name, bytes(text)));
        return result;
    }
}
