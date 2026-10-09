package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceDivergence.Finding;
import elite.intel.bindforge.devicefiles.DeviceDivergence.Issue;
import elite.intel.bindforge.devicefiles.DeviceDivergence.Severity;
import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.ElementChange;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.ElementKind;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.InstallationCheck;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.LabelChange;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.LabelKind;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.Report;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.State;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Target;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Before first setup the installations against each other; once the master exists, {@link AgainstMaster}.
 * <p>
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
        assertEquals(Issue.ENTRY_MISSING, findings.get(0).issue());
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

    /**
     * Once the master exists: each installation against the master, not against each other, with severity still
     * judged against the bindings (Alan, 2026-10-08).
     */
    @Nested
    class AgainstMaster {

        private static final Target STEAM = new Target(1, "STEAM", Path.of("steam", "ControlSchemes"), false);
        private static final Target EPIC = new Target(2, "EPIC", Path.of("epic", "ControlSchemes"), false);
        private static final HardwareId RVWAP_HARDWARE = new HardwareId("3344", "03F5");
        private static final HardwareId OLD_PID = new HardwareId("3344", "43F4");

        @Test
        void installationsMatchingTheMasterProduceNothingToShow() {
            assertTrue(DeviceDivergence.againstMaster(report(unchanged(STEAM), unchanged(EPIC)), Map.of(),
                    Set.of("RVWAP")).isEmpty());
        }

        /** Without a master, an empty list would read as "everything matches" - so it is refused. */
        @Test
        void aReportWithNoMasterIsRefused() {
            Report notSetUp = new Report(false, Set.of(), Set.of(), List.of());

            assertThrows(IllegalArgumentException.class,
                    () -> DeviceDivergence.againstMaster(notSetUp, Map.of(), Set.of()));
        }

        /** The measured case, against the master: a named device one installation lacks breaks the preset. */
        @Test
        void anEntryTheMasterHoldsAndAnInstallationLacksIsRedWhenABindingNamesIt() {
            Report report = report(unchanged(STEAM), edited(EPIC, element("RVWAP", ElementKind.MISSING)));

            Finding finding = only(DeviceDivergence.againstMaster(report, Map.of(), Set.of("RVWAP")));

            assertEquals(Issue.ENTRY_MISSING, finding.issue());
            assertEquals(Severity.RED, finding.severity());
            assertEquals(Set.of("2"), finding.installs(), "named by installation id, as the screen keys its names");
        }

        @Test
        void theSameGapIsYellowWhenNoBindingNamesIt() {
            Report report = report(edited(EPIC, element("LVWAP", ElementKind.MISSING)));

            assertEquals(Severity.YELLOW,
                    only(DeviceDivergence.againstMaster(report, Map.of(), Set.of("RVWAP"))).severity());
        }

        /** One problem in two installations is one row naming both, not two rows. */
        @Test
        void theSameProblemInSeveralInstallationsIsOneRow() {
            Report report = report(
                    edited(STEAM, element("RVWAP", ElementKind.HARDWARE_DIFFERS)),
                    edited(EPIC, element("RVWAP", ElementKind.HARDWARE_DIFFERS)));

            Finding finding = only(DeviceDivergence.againstMaster(report, Map.of(), Set.of("RVWAP")));

            assertEquals(Issue.ENTRY_HARDWARE_DIFFERS, finding.issue());
            assertEquals(Severity.RED, finding.severity());
            assertEquals(Set.of("1", "2"), finding.installs());
        }

        /** Two different problems with one device are two rows. */
        @Test
        void twoIssuesOnOneDeviceInOneInstallationAreTwoRows() {
            Report report = report(edited(STEAM, List.of(element("RVWAP", ElementKind.HARDWARE_DIFFERS)),
                    label("RVWAP", LabelKind.CHANGED, "Joy_1")));

            List<Finding> findings = DeviceDivergence.againstMaster(report, Map.of(), Set.of("RVWAP"));

            assertEquals(List.of(Issue.ENTRY_HARDWARE_DIFFERS, Issue.LABELS_DIFFER),
                    findings.stream().map(Finding::issue).toList());
            assertEquals(List.of(Severity.RED, Severity.YELLOW), findings.stream().map(Finding::severity).toList());
        }

        /** Below Frontier's entry for the same hardware, the user's name is never resolved - domain doc §1.2d. */
        @Test
        void anEntryOutOfPlaceIsRedWhenABindingNamesItAndYellowWhenNot() {
            Report report = report(edited(STEAM, element("RVWAP", ElementKind.OUT_OF_PLACE)));

            Finding named = only(DeviceDivergence.againstMaster(report, Map.of(), Set.of("RVWAP")));
            Finding unnamed = only(DeviceDivergence.againstMaster(report, Map.of(), Set.of()));

            assertEquals(Issue.ENTRY_OUT_OF_PLACE, named.issue());
            assertEquals(Severity.RED, named.severity());
            assertEquals(Severity.YELLOW, unnamed.severity());
        }

        /** Apply never spreads an entry the master lacks, so the installations without it stay broken. */
        @Test
        void aNamedEntryTheMasterDoesNotHoldIsRedAndItsLabelsAreNotListedSeparately() {
            Report report = report(edited(STEAM, List.of(element("Pedals", ElementKind.ADDED)),
                    label("Pedals", LabelKind.ADDED, "Joy_1"), label("Pedals", LabelKind.ADDED, "Joy_2")));

            Finding finding = only(DeviceDivergence.againstMaster(report, Map.of(), Set.of("Pedals")));

            assertEquals(Issue.ENTRY_NOT_IN_MASTER, finding.issue());
            assertEquals(Severity.RED, finding.severity());
        }

        @Test
        void anEntryTheMasterDoesNotHoldIsYellowWhenNoBindingNamesIt() {
            Report report = report(edited(STEAM, element("Pedals", ElementKind.ADDED)));

            Finding finding = only(DeviceDivergence.againstMaster(report, Map.of(), Set.of("RVWAP")));

            assertEquals(Issue.ENTRY_NOT_IN_MASTER, finding.issue());
            assertEquals(Severity.YELLOW, finding.severity());
        }

        /**
         * Reset to Frontier's file is one row - not one per entry it lost, nor one per {@code .buttonMap} - and red
         * when a binding names one of the user's own devices, which are what it lost.
         */
        @Test
        void aResetToFrontiersFileIsOneRowAndRedWhenABindingNamesAUserDevice() {
            Report report = report(Set.of("RVWAP", "T-Rudder"), Set.of("RVWAP"),
                    new InstallationCheck(EPIC, State.WIPED, null,
                            List.of(element("RVWAP", ElementKind.MISSING)),
                            List.of(new LabelChange("RVWAP", LabelKind.FILE_MISSING, null, null, null)), false));

            Finding finding = only(DeviceDivergence.againstMaster(report, Map.of(), Set.of("RVWAP")));

            assertEquals(Issue.FILE_RESET, finding.issue());
            assertEquals("DeviceMappings.xml", finding.deviceName());
            assertEquals(Severity.RED, finding.severity());
        }

        /** Frontier's entries are still in Frontier's file, so bindings naming only them still resolve. */
        @Test
        void aResetToFrontiersFileIsYellowWhenTheBindingsNameOnlyFrontiersDevices() {
            Report report = report(Set.of("RVWAP", "T-Rudder"), Set.of("RVWAP"),
                    new InstallationCheck(EPIC, State.WIPED, null, List.of(element("RVWAP", ElementKind.MISSING)),
                            List.of(), false));

            Finding finding = only(DeviceDivergence.againstMaster(report, Map.of(), Set.of("T-Rudder")));

            assertEquals(Issue.FILE_RESET, finding.issue());
            assertEquals(Severity.YELLOW, finding.severity());
        }

        /** With no file at all the game loads no bindings, whatever they name. */
        @Test
        void aMissingFileIsAlwaysRed() {
            Report report = report(Set.of("T-Rudder"), Set.of(),
                    new InstallationCheck(EPIC, State.WIPED, null, List.of(), List.of(), true));

            Finding finding = only(DeviceDivergence.againstMaster(report, Map.of(), Set.of()));

            assertEquals(Issue.FILE_RESET, finding.issue());
            assertEquals(Severity.RED, finding.severity());
        }

        @Test
        void anUnreadableFileIsRedWhenABindingNamesAnyMasterDeviceAndYellowOtherwise() {
            Report report = report(Set.of("RVWAP", "T-Rudder"), Set.of("RVWAP"),
                    new InstallationCheck(EPIC, State.UNREADABLE, "could not read", List.of(), List.of()));

            Finding named = only(DeviceDivergence.againstMaster(report, Map.of(), Set.of("T-Rudder")));
            Finding unnamed = only(DeviceDivergence.againstMaster(report, Map.of(), Set.of("Other")));

            assertEquals(Issue.FILE_UNREADABLE, named.issue());
            assertEquals(Severity.RED, named.severity());
            assertEquals(Severity.YELLOW, unnamed.severity());
        }

        /** An unplugged drive is not a divergence - nothing could be compared. */
        @Test
        void anInstallationWhoseFolderIsGoneIsNotListed() {
            Report report = report(new InstallationCheck(EPIC, State.MISSING, "folder not found", List.of(), List.of()));

            assertTrue(DeviceDivergence.againstMaster(report, Map.of(), Set.of("RVWAP")).isEmpty());
        }

        /** However many inputs differ, a device's labels are one row - and a label never breaks a binding. */
        @Test
        void differingLabelsAreOneYellowRowPerDevice() {
            Report report = report(edited(STEAM, List.of(),
                    label("RVWAP", LabelKind.CHANGED, "Joy_1"),
                    label("RVWAP", LabelKind.ADDED, "Joy_2"),
                    label("RVWAP", LabelKind.REMOVED, "Joy_3")));

            Finding finding = only(DeviceDivergence.againstMaster(report, Map.of(), Set.of("RVWAP")));

            assertEquals(Issue.LABELS_DIFFER, finding.issue());
            assertEquals(Severity.YELLOW, finding.severity());
            assertTrue(finding.referenced(), "still says a binding names it, though that does not make it red");
        }

        @Test
        void aMissingOrUnreadableButtonMapIsYellowAndALayoutOnlyDifferenceIsNotListed() {
            Report report = report(edited(STEAM, List.of(),
                            new LabelChange("RVWAP", LabelKind.FILE_MISSING, null, null, null),
                            new LabelChange("LVWAP", LabelKind.FILE_UNREADABLE, null, null, null)),
                    edited(EPIC, List.of(), new LabelChange("RVWAP", LabelKind.LAYOUT_ONLY, null, null, null)));

            List<Finding> findings = DeviceDivergence.againstMaster(report, Map.of(), Set.of("RVWAP", "LVWAP"));

            assertEquals(List.of(Issue.BUTTON_MAP_UNREADABLE, Issue.BUTTON_MAP_MISSING),
                    findings.stream().map(Finding::issue).toList());
            assertTrue(findings.stream().allMatch(finding -> finding.severity() == Severity.YELLOW));
            assertTrue(findings.stream().allMatch(finding -> finding.installs().equals(Set.of("1"))));
        }

        @Test
        void anOrphanedButtonMapIsYellow() {
            Finding finding = only(DeviceDivergence.againstMaster(report(unchanged(STEAM)),
                    Map.of("1", List.of(Path.of("steam", "LVWAP.buttonMap"))), Set.of("LVWAP")));

            assertEquals(Issue.ORPHANED_BUTTON_MAP, finding.issue());
            assertEquals("LVWAP", finding.deviceName());
            assertEquals(Severity.YELLOW, finding.severity());
        }

        /** The map an entry left behind is the same problem as the missing entry, so it is not listed twice. */
        @Test
        void theButtonMapOfAnEntryTheInstallationLacksIsNotAlsoAnOrphan() {
            Report report = report(edited(STEAM, element("RVWAP", ElementKind.MISSING)));

            List<Finding> findings = DeviceDivergence.againstMaster(report,
                    Map.of("1", List.of(Path.of("steam", "RVWAP.buttonMap"), Path.of("steam", "Old.buttonMap"))),
                    Set.of("RVWAP"));

            assertEquals(List.of(Issue.ENTRY_MISSING, Issue.ORPHANED_BUTTON_MAP),
                    findings.stream().map(Finding::issue).toList());
            assertEquals("Old", findings.get(1).deviceName());
        }

        @Test
        void redComesFirstAndNamesAreOrderedWithinASeverity() {
            Report report = report(edited(STEAM, List.of(
                            element("RVWAP", ElementKind.MISSING),
                            element("LVWAP", ElementKind.MISSING)),
                    label("Alpha", LabelKind.CHANGED, "Joy_1")));

            List<Finding> findings = DeviceDivergence.againstMaster(report, Map.of(), Set.of("RVWAP", "LVWAP"));

            assertEquals(List.of("LVWAP", "RVWAP", "Alpha"), findings.stream().map(Finding::deviceName).toList());
            assertEquals(List.of(Severity.RED, Severity.RED, Severity.YELLOW),
                    findings.stream().map(Finding::severity).toList());
        }

        private Report report(InstallationCheck... installations) {
            return report(Set.of("RVWAP", "LVWAP"), Set.of("RVWAP", "LVWAP"), installations);
        }

        private Report report(Set<String> masterNames, Set<String> userNames, InstallationCheck... installations) {
            return new Report(true, masterNames, userNames, List.of(installations));
        }

        private InstallationCheck unchanged(Target target) {
            return new InstallationCheck(target, State.UNCHANGED, null, List.of(), List.of());
        }

        private InstallationCheck edited(Target target, ElementChange element) {
            return edited(target, List.of(element));
        }

        private InstallationCheck edited(Target target, List<ElementChange> elements, LabelChange... labels) {
            return new InstallationCheck(target, State.EDITED, null, elements, List.of(labels));
        }

        private ElementChange element(String name, ElementKind kind) {
            return switch (kind) {
                case MISSING -> new ElementChange(name, kind, RVWAP_HARDWARE, null);
                case ADDED -> new ElementChange(name, kind, null, RVWAP_HARDWARE);
                case HARDWARE_DIFFERS -> new ElementChange(name, kind, RVWAP_HARDWARE, OLD_PID);
                case OUT_OF_PLACE -> new ElementChange(name, kind, RVWAP_HARDWARE, RVWAP_HARDWARE);
            };
        }

        private LabelChange label(String device, LabelKind kind, String input) {
            return new LabelChange(device, kind, input,
                    kind == LabelKind.ADDED ? null : "master", kind == LabelKind.REMOVED ? null : "installation");
        }

        private Finding only(List<Finding> findings) {
            assertEquals(1, findings.size(), () -> "expected one finding, got " + findings);
            return findings.get(0);
        }
    }
}
