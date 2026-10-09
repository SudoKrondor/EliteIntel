package elite.intel.bindforge.devices;

import elite.intel.bindforge.devicefiles.DeviceDivergence.Finding;
import elite.intel.bindforge.devicefiles.DeviceDivergence.Issue;
import elite.intel.bindforge.devicefiles.DeviceDivergence.Severity;
import elite.intel.bindforge.devicefiles.DeviceEntry;
import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devices.InstallationMarkers.Marker;
import elite.intel.bindforge.devices.InstallationMarkers.State;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The Installations markers: M, <em>not added</em>, or the divergence colour, per installation - read from the
 * divergence list's own findings so a marker and the row explaining it cannot disagree.
 */
class InstallationMarkersTest {

    private static final String STEAM = "1";
    private static final String EPIC = "3";
    private static final List<String> BOTH = List.of(STEAM, EPIC);

    private static final MyDevice LVWAP = new MyDevice("VIRPIL L", "LVWAP", "3344", "83F4", true, "LVWAP", false);
    private static final DeviceEntry LVWAP_ENTRY = entry("LVWAP", "3344", "83F4");

    @Test
    void aDeviceEveryInstallationHoldsAsTheMasterSaysMatchesEverywhere() {
        Map<String, Marker> markers = InstallationMarkers.of(LVWAP, BOTH, Set.of(),
                Map.of(STEAM, List.of(LVWAP_ENTRY), EPIC, List.of(LVWAP_ENTRY)), List.of());

        assertEquals(new Marker(State.MATCHES, Severity.GREEN), markers.get(STEAM));
        assertEquals(new Marker(State.MATCHES, Severity.GREEN), markers.get(EPIC));
        assertEquals(BOTH, List.copyOf(markers.keySet()), "one marker per installation, in the column order");
    }

    /** The text says what the installation holds; the colour says what it costs. */
    @Test
    void anEntryTheMasterHoldsAndOneInstallationLacksIsNotAddedInTheFindingsColour() {
        Map<String, Marker> markers = InstallationMarkers.of(LVWAP, BOTH, Set.of(),
                Map.of(STEAM, List.of(LVWAP_ENTRY), EPIC, List.of()),
                List.of(finding(Issue.ENTRY_MISSING, Severity.RED, "LVWAP", EPIC)));

        assertEquals(new Marker(State.MATCHES, Severity.GREEN), markers.get(STEAM));
        assertEquals(new Marker(State.NOT_ADDED, Severity.RED), markers.get(EPIC));
    }

    @Test
    void theWorstFindingForTheDeviceDecidesTheColour() {
        Map<String, Marker> markers = InstallationMarkers.of(LVWAP, List.of(STEAM), Set.of(),
                Map.of(STEAM, List.of(LVWAP_ENTRY)),
                List.of(finding(Issue.LABELS_DIFFER, Severity.YELLOW, "LVWAP", STEAM),
                        finding(Issue.ENTRY_OUT_OF_PLACE, Severity.RED, "LVWAP", STEAM)));

        assertEquals(new Marker(State.DIFFERS, Severity.RED), markers.get(STEAM));
    }

    @Test
    void aFindingAboutAnotherDeviceOrAnotherInstallationIsNotThisMarkersBusiness() {
        Map<String, Marker> markers = InstallationMarkers.of(LVWAP, BOTH, Set.of(),
                Map.of(STEAM, List.of(LVWAP_ENTRY), EPIC, List.of(LVWAP_ENTRY)),
                List.of(finding(Issue.ENTRY_MISSING, Severity.RED, "RVWAP", STEAM),
                        finding(Issue.LABELS_DIFFER, Severity.YELLOW, "LVWAP", EPIC)));

        assertEquals(State.MATCHES, markers.get(STEAM).state());
        assertEquals(new Marker(State.DIFFERS, Severity.YELLOW), markers.get(EPIC));
    }

    /** An entry the master does not hold is named only in the installation, so that name must count too. */
    @Test
    void anEntryOnlyTheInstallationHoldsDiffersUnderItsOwnName() {
        MyDevice notInMaster = new MyDevice(null, "Stick", "1234", "ABCD", false, null, false);
        Map<String, Marker> markers = InstallationMarkers.of(notInMaster, BOTH, Set.of(),
                Map.of(STEAM, List.of(entry("Stick", "1234", "abcd")), EPIC, List.of()),
                List.of(finding(Issue.ENTRY_NOT_IN_MASTER, Severity.YELLOW, "Stick", STEAM)));

        assertEquals(new Marker(State.DIFFERS, Severity.YELLOW), markers.get(STEAM));
        assertEquals(new Marker(State.NOT_ADDED, null), markers.get(EPIC),
                "neither the master nor Epic holds it - nothing to judge, so no colour");
    }

    /** An entry the master lacks with no finding - a pending removal - is still not the master's to match. */
    @Test
    void anEntryTheMasterDoesNotHoldNeverMatches() {
        MyDevice leftover = new MyDevice(null, "OldStick", "1234", "ABCD", false, null, false);
        Map<String, Marker> markers = InstallationMarkers.of(leftover, List.of(STEAM), Set.of(),
                Map.of(STEAM, List.of(entry("OldStick", "1234", "ABCD"))), List.of());

        assertEquals(new Marker(State.NOT_ADDED, null), markers.get(STEAM));
    }

    @Test
    void aBuiltInWithFrontiersEntryMatchesAndWithoutItIsNotAdded() {
        MyDevice rudder = new MyDevice("T-Rudder", null, "044F", "B679", true, null, true);
        Map<String, Marker> markers = InstallationMarkers.of(rudder, BOTH, Set.of(),
                Map.of(STEAM, List.of(entry("T-Rudder", "044f", "b679")), EPIC, List.of()), List.of());

        assertEquals(new Marker(State.MATCHES, Severity.GREEN), markers.get(STEAM));
        assertEquals(new Marker(State.NOT_ADDED, null), markers.get(EPIC), "its file is gone, so is Frontier's entry");
    }

    /**
     * The check compares only the user's elements, so Frontier's entry gone from an installation raises no finding -
     * a built-in the master labels must still find its entry before it reads M (review, 2026-10-08).
     */
    @Test
    void aBuiltInTheMasterLabelsMatchesOnlyWhereFrontiersEntryIsThere() {
        MyDevice labelled = new MyDevice("T-Rudder", null, "044F", "B679", true, "T-Rudder", true);
        Map<String, Marker> markers = InstallationMarkers.of(labelled, BOTH, Set.of(),
                Map.of(STEAM, List.of(entry("T-Rudder", "044F", "B679")), EPIC, List.of(LVWAP_ENTRY)), List.of());

        assertEquals(new Marker(State.MATCHES, Severity.GREEN), markers.get(STEAM));
        assertEquals(new Marker(State.NOT_ADDED, null), markers.get(EPIC));
    }

    /** The reset's one row reaches every device the master holds - a built-in it labels included. */
    @Test
    void aResetFileGivesABuiltInTheMasterLabelsTheResetsColour() {
        MyDevice labelled = new MyDevice("T-Rudder", null, "044F", "B679", true, "T-Rudder", true);
        Map<String, Marker> markers = InstallationMarkers.of(labelled, List.of(EPIC), Set.of(),
                Map.of(EPIC, List.of(entry("T-Rudder", "044F", "B679"))),
                List.of(finding(Issue.FILE_RESET, Severity.RED, "DeviceMappings.xml", EPIC)));

        assertEquals(new Marker(State.DIFFERS, Severity.RED), markers.get(EPIC),
                "Frontier's entry survived the reset, but the row's colour is the installation's");
    }

    @Test
    void anInstallationWhoseFolderIsGoneIsNotFoundAndUncoloured() {
        Map<String, Marker> markers = InstallationMarkers.of(LVWAP, BOTH, Set.of(EPIC),
                Map.of(STEAM, List.of(LVWAP_ENTRY)), List.of());

        assertEquals(new Marker(State.NOT_FOUND, null), markers.get(EPIC));
    }

    /** One row for the reset file; it reaches every device the master holds, and not the others. */
    @Test
    void aResetFileMarksEveryMasterDeviceInItsRowsColour() {
        MyDevice unnamed = new MyDevice("Pad", null, "1234", "ABCD", true, null, false);
        List<Finding> reset = List.of(finding(Issue.FILE_RESET, Severity.RED, "DeviceMappings.xml", EPIC));
        Map<String, List<DeviceEntry>> entries = Map.of(STEAM, List.of(LVWAP_ENTRY), EPIC, List.of());

        assertEquals(new Marker(State.DIFFERS, Severity.RED),
                InstallationMarkers.of(LVWAP, BOTH, Set.of(), entries, reset).get(EPIC));
        assertEquals(new Marker(State.NOT_ADDED, null),
                InstallationMarkers.of(unnamed, BOTH, Set.of(), entries, reset).get(EPIC));
    }

    @Test
    void anUnreadableFileMarksEveryDeviceSinceNothingIsKnown() {
        MyDevice unnamed = new MyDevice("Pad", null, "1234", "ABCD", true, null, false);
        List<Finding> unreadable = List.of(finding(Issue.FILE_UNREADABLE, Severity.YELLOW, "DeviceMappings.xml", EPIC));
        Map<String, List<DeviceEntry>> entries = Map.of(STEAM, List.of(LVWAP_ENTRY));

        assertEquals(new Marker(State.DIFFERS, Severity.YELLOW),
                InstallationMarkers.of(unnamed, BOTH, Set.of(), entries, unreadable).get(EPIC));
    }

    private static DeviceEntry entry(String name, String vid, String pid) {
        return new DeviceEntry(name, Set.of(new HardwareId(vid, pid)));
    }

    private static Finding finding(Issue issue, Severity severity, String deviceName, String install) {
        return new Finding(issue, severity, deviceName, Set.of(install), severity == Severity.RED);
    }
}
