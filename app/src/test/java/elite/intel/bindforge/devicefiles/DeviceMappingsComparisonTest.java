package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceMappingsComparison.Difference;
import elite.intel.bindforge.devicefiles.DeviceMappingsComparison.Kind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two installations are supposed to hold the same device entries. This reports where they do not, which is
 * the raw material the divergence list ranks.
 */
class DeviceMappingsComparisonTest {

    private static final DeviceEntry.HardwareId VIRPIL = new DeviceEntry.HardwareId("3344", "83F4");
    private static final DeviceEntry.HardwareId THROTTLE = new DeviceEntry.HardwareId("3344", "0197");

    @Test
    void installationsHoldingTheSameEntriesHaveNothingToReport() {
        List<Difference> differences = DeviceMappingsComparison.compare(Map.of(
                "steam", List.of(entry("RVWAP", VIRPIL)),
                "epic", List.of(entry("RVWAP", VIRPIL))));

        assertTrue(differences.isEmpty(), "matching is the state BindForge is trying to reach");
    }

    /**
     * The measured case: an entry one installation has and another does not. A shared {@code .binds} naming
     * that device resolves to nothing in the installation that lacks it.
     */
    @Test
    void anEntryMissingFromOneInstallationIsReported() {
        List<Difference> differences = DeviceMappingsComparison.compare(Map.of(
                "steam", List.of(entry("RVWAP", VIRPIL), entry("VPCThrottle", THROTTLE)),
                "epic", List.of(entry("VPCThrottle", THROTTLE))));

        assertEquals(1, differences.size());
        Difference difference = differences.get(0);
        assertEquals("RVWAP", difference.deviceName());
        assertEquals(Kind.MISSING_FROM_SOME, difference.kind());
        assertEquals(Set.of("steam"), difference.presentIn());
        assertEquals(Set.of("epic"), difference.absentFrom());
    }

    /**
     * The dangerous shape: the same name in both installations pointing at different hardware. Bindings
     * resolve in both, to different sticks.
     */
    @Test
    void oneNamePointingAtDifferentHardwareIsReported() {
        List<Difference> differences = DeviceMappingsComparison.compare(Map.of(
                "steam", List.of(entry("RVWAP", VIRPIL)),
                "epic", List.of(entry("RVWAP", THROTTLE))));

        assertEquals(1, differences.size());
        assertEquals(Kind.HARDWARE_DIFFERS, differences.get(0).kind());
        assertEquals(Set.of(VIRPIL), differences.get(0).hardwareByInstall().get("steam"));
        assertEquals(Set.of(THROTTLE), differences.get(0).hardwareByInstall().get("epic"));
    }

    /** Hex case is not a convention in Frontier's file, so it cannot be a difference either. */
    @Test
    void theSameHardwareSpelledInDifferentCasesIsNotADifference() {
        List<Difference> differences = DeviceMappingsComparison.compare(Map.of(
                "steam", List.of(entry("RVWAP", new DeviceEntry.HardwareId("3344", "83f4"))),
                "epic", List.of(entry("RVWAP", new DeviceEntry.HardwareId("3344", "83F4")))));

        assertTrue(differences.isEmpty());
    }

    @Test
    void anEntryAbsentFromSeveralInstallationsNamesEachOfThem() {
        List<Difference> differences = DeviceMappingsComparison.compare(Map.of(
                "steam", List.of(entry("RVWAP", VIRPIL)),
                "epic", List.of(),
                "frontier", List.of()));

        assertEquals(Set.of("epic", "frontier"), differences.get(0).absentFrom());
        assertEquals(Set.of("steam"), differences.get(0).presentIn());
    }

    @Test
    void differencesComeBackInDeviceNameOrderSoTheListIsStable() {
        List<Difference> differences = DeviceMappingsComparison.compare(Map.of(
                "steam", List.of(entry("Zebra", VIRPIL), entry("Alpha", VIRPIL)),
                "epic", List.of()));

        assertEquals(List.of("Alpha", "Zebra"), differences.stream().map(Difference::deviceName).toList());
    }

    /** One installation cannot disagree with itself, so there is nothing to compare. */
    @Test
    void aSingleInstallationProducesNoDifferences() {
        assertTrue(DeviceMappingsComparison.compare(Map.of("steam", List.of(entry("RVWAP", VIRPIL)))).isEmpty());
    }

    private DeviceEntry entry(String name, DeviceEntry.HardwareId... hardware) {
        return new DeviceEntry(name, Set.of(hardware));
    }
}
