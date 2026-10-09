package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.ElementChange;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.ElementKind;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.InstallationCheck;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.LabelChange;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.LabelKind;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.Report;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.State;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.MasterDevice;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Target;
import elite.intel.io.TimestampedBackups;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The startup check for the device files: each installation against the saved master, and each outcome found
 * exactly - unchanged, wiped or edited, with the edit shown as its changes (Alan, 2026-10-05).
 */
class DeviceFilesCheckTest {

    private static final String DEVICE_MAPPINGS = """
            <?xml version="1.0" encoding="UTF-8" ?>
            <Root>
            \t<GamePad>
            \t\t<PID>028E</PID><VID>045E</VID>
            \t</GamePad>
            \t<T-Rudder><PID>B679</PID><VID>044F</VID></T-Rudder>
            </Root>
            """;

    private static final MasterDevice RVWAP =
            new MasterDevice("RVWAP", "3344", "43F4", Map.of("Joy_1", "RV Main Trigger", "Joy_2", "Pinky"));
    private static final MasterDevice LVWAP = new MasterDevice("LVWAP", "3344", "83F4", Map.of());

    @TempDir
    Path temp;

    private final Set<String> leftovers = new HashSet<>();

    @Test
    void anEmptyMasterMeansFirstSetupHasNotRunAndNothingIsCompared() throws IOException {
        Target steam = installation(1, "STEAM");

        Report report = check(List.of(steam), List.of());

        assertFalse(report.setUp());
        assertTrue(report.installations().isEmpty());
    }

    @Test
    void anInstallationThePushBroughtUpToDateIsUnchanged() throws IOException {
        Target steam = pushed(installation(1, "STEAM"), List.of(RVWAP, LVWAP));

        InstallationCheck result = only(check(List.of(steam), List.of(RVWAP, LVWAP)));

        assertEquals(State.UNCHANGED, result.state());
        assertTrue(result.elements().isEmpty());
        assertTrue(result.labels().isEmpty());
    }

    @Test
    void anInstallationWhoseFolderIsGoneIsReportedNotCompared() {
        Target gone = new Target(1, "EPIC", temp.resolve("unplugged").resolve("ControlSchemes"), false);

        InstallationCheck result = only(check(List.of(gone), List.of(RVWAP)));

        assertEquals(State.MISSING, result.state());
        assertTrue(result.reason().contains("folder not found"), result.reason());
    }

    @Test
    void noDeviceMappingsAtAllIsAWipe() throws IOException {
        Target steam = pushed(installation(1, "STEAM"), List.of(RVWAP));
        Files.delete(deviceMappings(steam));

        InstallationCheck result = only(check(List.of(steam), List.of(RVWAP)));

        assertEquals(State.WIPED, result.state());
        assertTrue(result.fileGone(), "no file at all, which the divergence list colours apart from a stock one");
        assertTrue(result.elements().contains(new ElementChange("RVWAP", ElementKind.MISSING,
                new HardwareId("3344", "43F4"), null)));
    }

    /** The incident BindForge exists for: a patch puts Frontier's file back over the user's. */
    @Test
    void frontiersStockFileInPlaceOfTheUsersIsAWipe() throws IOException {
        Target steam = pushed(installation(1, "STEAM"), List.of(RVWAP));
        Files.write(deviceMappings(steam), DeviceFilesPush.stockFile());

        InstallationCheck result = only(check(List.of(steam), List.of(RVWAP)));

        assertEquals(State.WIPED, result.state());
        assertFalse(result.fileGone(), "Frontier's file is there");
    }

    /** The report says which master it compared against, and which of its names Frontier does not ship. */
    @Test
    void theReportCarriesTheMastersNamesAndTheUsersOwn() throws IOException {
        MasterDevice rudderLabels = new MasterDevice("T-Rudder", "044F", "B679", Map.of("Joy_1", "Brake"));
        Target steam = pushed(installation(1, "STEAM"), List.of(RVWAP, rudderLabels));

        Report report = check(List.of(steam), List.of(RVWAP, rudderLabels));

        assertEquals(Set.of("RVWAP", "T-Rudder"), report.masterNames());
        assertEquals(Set.of("RVWAP"), report.userNames());
    }

    /**
     * The file carries no version, so a newer stock file reads as an edit - with every entry of the user's listed
     * as gone, which is what makes it recognisable. Never adopted on its own, so this fails safe.
     */
    @Test
    void aStockFileThatIsNotByteIdenticalIsAnEditListingEveryEntryAsMissing() throws IOException {
        Target steam = pushed(installation(1, "STEAM"), List.of(RVWAP));
        String stock = new String(DeviceFilesPush.stockFile(), StandardCharsets.UTF_8);
        Files.writeString(deviceMappings(steam), stock.replace("<Root>", "<Root>\r\n\t<!-- a later game version -->"));

        InstallationCheck result = only(check(List.of(steam), List.of(RVWAP)));

        assertEquals(State.EDITED, result.state());
        assertEquals(ElementKind.MISSING, result.elements().getFirst().kind());
    }

    /** A master holding only labels on Frontier's own entry expects the stock file, so it is not a wipe. */
    @Test
    void theStockFileIsNotAWipeWhenTheMasterPutsNoElementOfTheUsersIntoIt() throws IOException {
        MasterDevice rudder = new MasterDevice("T-Rudder", "044F", "B679", Map.of("Joy_RZAxis", "Rudder"));
        Target steam = installation(1, "STEAM");
        Files.write(deviceMappings(steam), DeviceFilesPush.stockFile());

        InstallationCheck result = only(check(List.of(steam), List.of(rudder)));

        assertEquals(State.EDITED, result.state());
        assertEquals(List.of(new LabelChange("T-Rudder", LabelKind.FILE_MISSING, null, null, null)), result.labels());
        assertTrue(result.elements().isEmpty(), "Frontier's own elements are never compared");
    }

    @Test
    void anEditIsReportedAsItsChanges() throws IOException {
        Target steam = pushed(installation(1, "STEAM"), List.of(RVWAP, LVWAP));
        String file = read(deviceMappings(steam))
                .replace("<PID>83F4</PID>", "<PID>83F5</PID>")
                .replace("<GamePad>", "<STICK2><PID>0001</PID><VID>1234</VID></STICK2><GamePad>");
        Files.writeString(deviceMappings(steam), file);
        Files.writeString(buttonMap(steam, "RVWAP"),
                "<Root><Joy_1>TRIGGER</Joy_1><Joy_3>HAT</Joy_3></Root>");
        Files.writeString(buttonMap(steam, "STICK2"), "<Root><Joy_1>FIRE</Joy_1></Root>");

        InstallationCheck result = only(check(List.of(steam), List.of(RVWAP, LVWAP)));

        assertEquals(State.EDITED, result.state());
        assertTrue(result.elements().contains(new ElementChange("LVWAP", ElementKind.HARDWARE_DIFFERS,
                new HardwareId("3344", "83F4"), new HardwareId("3344", "83F5"))), result.elements().toString());
        assertTrue(result.elements().contains(new ElementChange("STICK2", ElementKind.ADDED, null,
                new HardwareId("1234", "0001"))), result.elements().toString());
        assertEquals(Set.of(
                        new LabelChange("RVWAP", LabelKind.CHANGED, "Joy_1", "RV Main Trigger", "TRIGGER"),
                        new LabelChange("RVWAP", LabelKind.REMOVED, "Joy_2", "Pinky", null),
                        new LabelChange("RVWAP", LabelKind.ADDED, "Joy_3", null, "HAT"),
                        new LabelChange("STICK2", LabelKind.ADDED, "Joy_1", null, "FIRE")),
                Set.copyOf(result.labels()));
    }

    /** Below Frontier's entry for the same hardware, the user's element is never the one the game uses. */
    @Test
    void aUserElementThatNoLongerLeadsTheFileIsOutOfPlace() throws IOException {
        Target steam = pushed(installation(1, "STEAM"), List.of(RVWAP));
        String file = read(deviceMappings(steam));
        String element = file.substring(file.indexOf("<RVWAP>"), file.indexOf("</RVWAP>") + "</RVWAP>".length());
        Files.writeString(deviceMappings(steam),
                file.replace(element, "").replace("</Root>", element + "\r\n</Root>"));

        InstallationCheck result = only(check(List.of(steam), List.of(RVWAP)));

        assertEquals(State.EDITED, result.state());
        assertEquals(List.of(ElementKind.OUT_OF_PLACE), result.elements().stream().map(ElementChange::kind).toList());
    }

    /** Apply never removes, so a pending removal or a rename's old name lingers - and is not the user adding it. */
    @Test
    void anElementExpectedToLingerIsNotReportedAsAdded() throws IOException {
        Target steam = pushed(installation(1, "STEAM"), List.of(RVWAP, LVWAP));
        leftovers.add("LVWAP");

        InstallationCheck result = only(check(List.of(steam), List.of(RVWAP)));

        assertEquals(State.UNCHANGED, result.state());
    }

    /**
     * Apply never removes, so an entry added by hand leaves the push with nothing to write - and the installation
     * still differs from the master. Unchanged has to mean both.
     */
    @Test
    void anEntryAddedByHandIsAnEditEvenThoughApplyWouldWriteNothing() throws IOException {
        Target steam = pushed(installation(1, "STEAM"), List.of(RVWAP));
        Files.writeString(deviceMappings(steam), read(deviceMappings(steam))
                .replace("</Root>", "<STICK2><PID>0001</PID><VID>1234</VID></STICK2></Root>"));

        InstallationCheck result = only(check(List.of(steam), List.of(RVWAP)));

        assertEquals(State.EDITED, result.state());
        assertEquals(List.of(new ElementChange("STICK2", ElementKind.ADDED, null, new HardwareId("1234", "0001"))),
                result.elements());
    }

    @Test
    void aDeviceMappingsThatCannotBeReadIsReportedNotCompared() throws IOException {
        Target steam = installation(1, "STEAM");
        Files.writeString(deviceMappings(steam), "<Root><unclosed></Root>");

        InstallationCheck result = only(check(List.of(steam), List.of(RVWAP)));

        assertEquals(State.UNREADABLE, result.state());
    }

    @Test
    void aButtonMapThatCannotBeReadIsSaidSoRatherThanGuessed() throws IOException {
        Target steam = pushed(installation(1, "STEAM"), List.of(RVWAP));
        Files.writeString(buttonMap(steam, "RVWAP"), "<Root><Joy_1>broken</Root>");

        InstallationCheck result = only(check(List.of(steam), List.of(RVWAP)));

        assertEquals(List.of(new LabelChange("RVWAP", LabelKind.FILE_UNREADABLE, null, null, null)), result.labels());
    }

    @Test
    void sameLabelsInADifferentLayoutAreReportedAsLayoutOnly() throws IOException {
        Target steam = pushed(installation(1, "STEAM"), List.of(RVWAP));
        Files.writeString(buttonMap(steam, "RVWAP"),
                "<Root>\n  <Joy_2>Pinky</Joy_2>\n  <Joy_1>RV Main Trigger</Joy_1>\n</Root>");

        InstallationCheck result = only(check(List.of(steam), List.of(RVWAP)));

        assertEquals(List.of(new LabelChange("RVWAP", LabelKind.LAYOUT_ONLY, null, null, null)), result.labels());
    }

    @Test
    void oneInstallationIsCheckedById() throws IOException {
        Target steam = pushed(installation(1, "STEAM"), List.of(RVWAP));
        Target epic = installation(2, "EPIC");
        DeviceFilesCheck check = checker(List.of(steam, epic), List.of(RVWAP));

        assertEquals(State.EDITED, check.check(2).orElseThrow().state());
        assertTrue(check.check(9).isEmpty());
    }

    private Report check(List<Target> targets, List<MasterDevice> master) {
        return checker(targets, master).check();
    }

    private DeviceFilesCheck checker(List<Target> targets, List<MasterDevice> master) {
        return new DeviceFilesCheck(() -> targets, () -> master, () -> leftovers, FrontierStockDevices.getInstance());
    }

    private Target pushed(Target target, List<MasterDevice> master) {
        new DeviceFilesPush(() -> List.of(target), () -> master, FrontierStockDevices.getInstance(),
                new TimestampedBackups(), temp.resolve("history"), () -> 10, written -> { }).push();
        return target;
    }

    private Target installation(long id, String storefront) throws IOException {
        Path controlSchemes = temp.resolve(storefront).resolve("ControlSchemes");
        Files.createDirectories(controlSchemes.resolve("DeviceButtonMaps"));
        Files.writeString(controlSchemes.resolve("DeviceMappings.xml"), DEVICE_MAPPINGS);
        return new Target(id, storefront, controlSchemes, false);
    }

    private static InstallationCheck only(Report report) {
        assertEquals(1, report.installations().size());
        return report.installations().getFirst();
    }

    private static Path deviceMappings(Target target) {
        return target.controlSchemes().resolve("DeviceMappings.xml");
    }

    private static Path buttonMap(Target target, String device) {
        return target.controlSchemes().resolve("DeviceButtonMaps").resolve(device + ".buttonMap");
    }

    private static String read(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
