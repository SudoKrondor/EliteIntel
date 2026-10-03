package elite.intel.bindforge.devices;

import elite.intel.bindforge.devices.DeviceDivergence.Finding;
import elite.intel.bindforge.devices.DeviceDivergence.Issue;
import elite.intel.bindforge.devices.DeviceDivergence.Severity;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Severity is judged against the bindings, which is the whole rule: the same divergence is a broken game or a
 * piece of untidiness depending on whether anything binds to that device.
 */
class DeviceDivergenceTest {

    private static final DeviceEntry.HardwareId VIRPIL = new DeviceEntry.HardwareId("3344", "83F4");
    private static final DeviceEntry.HardwareId THROTTLE = new DeviceEntry.HardwareId("3344", "0197");

    /**
     * The measured case: 144 bindings naming two sticks one installation had never heard of, and the whole
     * preset rejected.
     */
    @Test
    void anEntryMissingFromAnInstallationIsRedWhenABindingNamesIt() {
        List<Finding> findings = DeviceDivergence.rank(
                missingFromEpic("RVWAP"), Map.of(), Set.of("RVWAP"));

        assertEquals(1, findings.size());
        assertEquals(Severity.RED, findings.get(0).severity());
        assertEquals(Issue.ENTRY_MISSING_FROM_SOME, findings.get(0).issue());
        assertEquals(Set.of("epic"), findings.get(0).installs());
        assertTrue(findings.get(0).referenced());
    }

    /** The same divergence, with nothing bound to it: the game never looks it up, so nothing breaks. */
    @Test
    void theSameGapIsYellowWhenNoBindingNamesIt() {
        List<Finding> findings = DeviceDivergence.rank(
                missingFromEpic("RVWAP"), Map.of(), Set.of());

        assertEquals(Severity.YELLOW, findings.get(0).severity());
    }

    @Test
    void oneNameOnDifferentHardwareIsRedWhenABindingNamesIt() {
        DeviceMappingsComparison.Difference difference = new DeviceMappingsComparison.Difference(
                "RVWAP", DeviceMappingsComparison.Kind.HARDWARE_DIFFERS,
                Set.of("steam", "epic"), Set.of(),
                Map.of("steam", Set.of(VIRPIL), "epic", Set.of(THROTTLE)));

        List<Finding> findings = DeviceDivergence.rank(List.of(difference), Map.of(), Set.of("RVWAP"));

        assertEquals(Severity.RED, findings.get(0).severity());
        assertEquals(Issue.ENTRY_HARDWARE_DIFFERS, findings.get(0).issue());
        assertEquals(Set.of("steam", "epic"), findings.get(0).installs(),
                "both installations are involved, unlike a missing entry");
    }

    /**
     * Nothing resolves through an orphan, so no binding can be broken by one however many bindings exist.
     */
    @Test
    void anOrphanedButtonMapIsAlwaysYellow() {
        List<Finding> findings = DeviceDivergence.rank(List.of(),
                Map.of("epic", List.of(Path.of("C:/EpicLibrary/LVWAP.buttonMap"))),
                Set.of("LVWAP"));

        assertEquals(1, findings.size());
        assertEquals(Severity.YELLOW, findings.get(0).severity());
        assertEquals(Issue.ORPHANED_BUTTON_MAP, findings.get(0).issue());
        assertEquals("LVWAP", findings.get(0).deviceName(), "reported by name, not by file path");
        assertEquals(Set.of("epic"), findings.get(0).installs());
    }

    @Test
    void redComesBeforeYellowAndNamesAreOrderedWithinASeverity() {
        List<Finding> findings = DeviceDivergence.rank(
                List.of(difference("Zebra", Set.of("epic")), difference("Alpha", Set.of("epic"))),
                Map.of("epic", List.of(Path.of("Orphan.buttonMap"))),
                Set.of("Zebra", "Alpha"));

        assertEquals(List.of("Alpha", "Zebra", "Orphan"),
                findings.stream().map(Finding::deviceName).toList());
        assertEquals(List.of(Severity.RED, Severity.RED, Severity.YELLOW),
                findings.stream().map(Finding::severity).toList());
    }

    @Test
    void installationsThatAgreeProduceNothingToShow() {
        assertTrue(DeviceDivergence.rank(List.of(), Map.of(), Set.of("RVWAP")).isEmpty());
    }

    private List<DeviceMappingsComparison.Difference> missingFromEpic(String deviceName) {
        return List.of(difference(deviceName, Set.of("epic")));
    }

    private DeviceMappingsComparison.Difference difference(String deviceName, Set<String> absentFrom) {
        return new DeviceMappingsComparison.Difference(deviceName,
                DeviceMappingsComparison.Kind.MISSING_FROM_SOME,
                Set.of("steam"), absentFrom, Map.of("steam", Set.of(VIRPIL)));
    }
}
