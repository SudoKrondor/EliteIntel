package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devicefiles.DeviceFilesAdopt.Conflict;
import elite.intel.bindforge.devicefiles.DeviceFilesAdopt.ConflictKind;
import elite.intel.bindforge.devicefiles.DeviceFilesAdopt.Outcome;
import elite.intel.bindforge.devicefiles.DeviceFilesAdopt.Result;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.ElementChange;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.ElementKind;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.InstallationCheck;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.LabelChange;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.LabelKind;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.State;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Target;
import elite.intel.db.managers.BindForgeDeviceDraftManager.AdoptionWrite;
import elite.intel.db.managers.BindForgeDeviceDraftManager.DraftWrite;
import elite.intel.db.managers.BindForgeDeviceDraftManager.Stored;
import elite.intel.db.managers.BindForgeDeviceDraftManager.StoredDevice;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Adopt: what one installation holds becomes the master - never a game file - and a draft saved on top of the old
 * master keeps every edit the user made (Alan, 2026-10-05).
 */
class DeviceFilesAdoptTest {

    private static final Target STEAM = new Target(1, "STEAM", Path.of("Steam", "ControlSchemes"), false);

    private static final StoredDevice LVWAP =
            new StoredDevice(10L, "LVWAP", "3344", "83F4", true, null, Map.of("Joy_1", "LV TRIGGER", "Joy_2", "PINKY"));
    private static final StoredDevice RVWAP = new StoredDevice(11L, "RVWAP", "3344", "43F4", true, null, Map.of());

    private final List<AdoptionWrite> written = new ArrayList<>();
    private int storeCalls;

    @Test
    void anUnchangedInstallationHasNothingToAdopt() {
        Result result = adopt(installation(State.UNCHANGED, List.of(), List.of()), stored(null));

        assertEquals(Outcome.NOTHING_TO_ADOPT, result.outcome());
        assertEquals(0, storeCalls);
    }

    /** Adopting a wipe would empty the master into Frontier's file. Its repair is revert. */
    @Test
    void aWipedInstallationIsRefused() {
        Result result = adopt(installation(State.WIPED, List.of(), List.of()), stored(null));

        assertEquals(Outcome.REFUSED, result.outcome());
        assertTrue(result.reason().contains("revert"), result.reason());
        assertEquals(0, storeCalls);
    }

    @Test
    void aButtonMapThatCannotBeReadRefusesTheAdoptRatherThanDropTheLabels() {
        Result result = adopt(installation(State.EDITED, List.of(),
                List.of(new LabelChange("LVWAP", LabelKind.FILE_UNREADABLE, null, null, null))), stored(null));

        assertEquals(Outcome.REFUSED, result.outcome());
        assertTrue(result.reason().contains("LVWAP.buttonMap"), result.reason());
        assertEquals(0, storeCalls);
    }

    @Test
    void theMasterTakesWhatTheInstallationHolds() {
        InstallationCheck edited = installation(State.EDITED,
                List.of(new ElementChange("LVWAP", ElementKind.HARDWARE_DIFFERS,
                                new HardwareId("3344", "83F4"), new HardwareId("3344", "83F5")),
                        new ElementChange("RVWAP", ElementKind.MISSING, new HardwareId("3344", "43F4"), null),
                        new ElementChange("STICK2", ElementKind.ADDED, null, new HardwareId("1234", "0001"))),
                List.of(new LabelChange("LVWAP", LabelKind.CHANGED, "Joy_1", "LV TRIGGER", "TRIGGER"),
                        new LabelChange("LVWAP", LabelKind.REMOVED, "Joy_2", "PINKY", null),
                        new LabelChange("STICK2", LabelKind.ADDED, "Joy_1", null, "FIRE")));

        Result result = adopt(edited, stored(null));

        assertEquals(Outcome.ADOPTED, result.outcome());
        assertEquals(List.of("RVWAP"), result.removed());
        assertEquals(List.of("STICK2"), result.added());
        AdoptionWrite write = written.getFirst();
        assertEquals(List.of("RVWAP"), write.removed(), "it is still in the other installations: a pending removal");
        assertNull(write.draft(), "with no draft started, none is written");
        StoredDevice lvwap = device(write.master(), "LVWAP");
        assertEquals("83F5", lvwap.pid());
        assertEquals(Map.of("Joy_1", "TRIGGER"), lvwap.labels());
        StoredDevice stick = device(write.master(), "STICK2");
        assertEquals(new HardwareId("1234", "0001"), new HardwareId(stick.vid(), stick.pid()));
        assertEquals(Map.of("Joy_1", "FIRE"), stick.labels());
        assertTrue(stick.aliasConfirmed(), "the user wrote the name and was shown it before adopting");
    }

    @Test
    void aMissingButtonMapIsAdoptedAsNoLabels() {
        Result result = adopt(installation(State.EDITED, List.of(),
                List.of(new LabelChange("LVWAP", LabelKind.FILE_MISSING, null, null, null))), stored(null));

        assertEquals(Outcome.ADOPTED, result.outcome());
        assertTrue(device(written.getFirst().master(), "LVWAP").labels().isEmpty());
    }

    /** Order and layout are where Apply puts things, not something the user chose - there is nothing to take. */
    @Test
    void onlyOrderOrLayoutDifferingHasNothingToAdopt() {
        Result result = adopt(installation(State.EDITED,
                List.of(new ElementChange("RVWAP", ElementKind.OUT_OF_PLACE, null, null)),
                List.of(new LabelChange("LVWAP", LabelKind.LAYOUT_ONLY, null, null, null))), stored(null));

        assertEquals(Outcome.NOTHING_TO_ADOPT, result.outcome());
        assertTrue(written.isEmpty());
    }

    /**
     * The draft rule: what the draft left alone takes the adopted value, what only the draft changed keeps the
     * draft's, and what both changed differently keeps the draft's and is reported.
     */
    @Test
    void aDraftKeepsItsOwnEditsAndTakesTheRest() {
        StoredDevice draftLvwap = new StoredDevice(10L, "LVWAP", "3344", "83F4", true, null,
                Map.of("Joy_1", "MY TRIGGER", "Joy_2", "MY PINKY"));
        StoredDevice draftRvwap = new StoredDevice(11L, "RVWAP", "3344", "43F4", true, null, Map.of());
        InstallationCheck edited = installation(State.EDITED,
                List.of(new ElementChange("LVWAP", ElementKind.HARDWARE_DIFFERS,
                        new HardwareId("3344", "83F4"), new HardwareId("3344", "83F5"))),
                List.of(new LabelChange("LVWAP", LabelKind.CHANGED, "Joy_1", "LV TRIGGER", "GAME TRIGGER"),
                        new LabelChange("LVWAP", LabelKind.ADDED, "Joy_3", null, "HAT")));

        Result result = adopt(edited, stored(List.of(draftLvwap, draftRvwap)));

        DraftWrite lvwap = draftNamed(written.getFirst().draft(), "LVWAP");
        assertEquals("LVWAP", lvwap.masterName());
        assertEquals("83F5", lvwap.device().pid(), "the draft left the hardware alone, so it takes the adopted");
        assertEquals(Map.of("Joy_1", "MY TRIGGER", "Joy_2", "MY PINKY", "Joy_3", "HAT"), lvwap.device().labels());
        assertEquals(List.of(new Conflict("LVWAP", ConflictKind.LABEL, "Joy_1")), result.conflicts(),
                "Joy_2 changed in the draft only, Joy_3 in the installation only - only Joy_1 changed on both");
        assertEquals(draftRvwap, draftNamed(written.getFirst().draft(), "RVWAP").device());
    }

    @Test
    void aDeviceTheInstallationLostLeavesTheDraftUnlessTheDraftChangedIt() {
        StoredDevice draftLvwap = new StoredDevice(10L, "LVWAP", "3344", "83F4", true, null,
                Map.of("Joy_1", "LV TRIGGER", "Joy_2", "PINKY"));
        StoredDevice draftRvwap = new StoredDevice(11L, "RVWAP", "3344", "43F5", true, null, Map.of());
        InstallationCheck edited = installation(State.EDITED,
                List.of(new ElementChange("LVWAP", ElementKind.MISSING, new HardwareId("3344", "83F4"), null),
                        new ElementChange("RVWAP", ElementKind.MISSING, new HardwareId("3344", "43F4"), null)),
                List.of());

        Result result = adopt(edited, stored(List.of(draftLvwap, draftRvwap)));

        List<DraftWrite> draft = written.getFirst().draft();
        assertEquals(1, draft.size(), draft.toString());
        assertNull(draft.getFirst().masterName(), "kept, it no longer comes from a master device");
        assertEquals("RVWAP", draft.getFirst().device().deviceName());
        assertEquals(List.of(new Conflict("RVWAP", ConflictKind.REMOVED_IN_INSTALLATION, null)), result.conflicts());
    }

    @Test
    void aDeviceTheDraftRemovedStaysRemovedAndAnInstallationChangeToItIsReported() {
        StoredDevice draftRvwap = new StoredDevice(11L, "RVWAP", "3344", "43F4", true, null, Map.of());
        InstallationCheck edited = installation(State.EDITED, List.of(),
                List.of(new LabelChange("LVWAP", LabelKind.CHANGED, "Joy_1", "LV TRIGGER", "GAME TRIGGER")));

        Result result = adopt(edited, stored(List.of(draftRvwap)));

        assertEquals(List.of("RVWAP"), written.getFirst().draft().stream().map(row -> row.device().deviceName()).toList());
        assertEquals(List.of(new Conflict("LVWAP", ConflictKind.REMOVED_IN_DRAFT, null)), result.conflicts());
    }

    @Test
    void anAddedDeviceJoinsTheDraftAndADraftAdditionIsKept() {
        StoredDevice draftOnly = new StoredDevice(null, "TRudder2", "044F", "B679", false, null, Map.of());
        InstallationCheck edited = installation(State.EDITED,
                List.of(new ElementChange("STICK2", ElementKind.ADDED, null, new HardwareId("1234", "0001"))),
                List.of());

        adopt(edited, stored(List.of(LVWAP, RVWAP, draftOnly)));

        List<DraftWrite> draft = written.getFirst().draft();
        assertEquals("STICK2", draftNamed(draft, "STICK2").masterName());
        assertNull(draftNamed(draft, "TRudder2").masterName());
        assertEquals(4, draft.size());
    }

    /** A draft device renamed to the name the installation added keeps it; adopt never renames. */
    @Test
    void anAddedDeviceUnderANameTheDraftAlreadyUsesIsAConflict() {
        StoredDevice renamed = new StoredDevice(11L, "STICK2", "3344", "43F4", true, "RVWAP", Map.of());
        InstallationCheck edited = installation(State.EDITED,
                List.of(new ElementChange("STICK2", ElementKind.ADDED, null, new HardwareId("1234", "0001"))),
                List.of());

        Result result = adopt(edited, stored(List.of(LVWAP, renamed)));

        assertTrue(result.conflicts().contains(new Conflict("STICK2", ConflictKind.NAME_TAKEN, null)),
                result.conflicts().toString());
        DraftWrite kept = draftNamed(written.getFirst().draft(), "STICK2");
        assertEquals("RVWAP", kept.masterName(), "still the renamed RVWAP, not the installation's device");
        assertEquals("43F4", kept.device().pid());
    }

    private Result adopt(InstallationCheck installation, Stored stored) {
        DeviceFilesAdopt adopt = new DeviceFilesAdopt(id -> Optional.of(installation), plan -> {
            storeCalls++;
            AdoptionWrite write = plan.apply(stored);
            if (write != null) written.add(write);
        });
        return adopt.adopt(STEAM.installId());
    }

    private static Stored stored(List<StoredDevice> draft) {
        return new Stored(List.of(LVWAP, RVWAP), draft);
    }

    private static InstallationCheck installation(State state, List<ElementChange> elements, List<LabelChange> labels) {
        return new InstallationCheck(STEAM, state, null, elements, labels);
    }

    private static StoredDevice device(List<StoredDevice> devices, String name) {
        return devices.stream().filter(device -> device.deviceName().equals(name)).findFirst().orElseThrow();
    }

    private static DraftWrite draftNamed(List<DraftWrite> draft, String name) {
        return draft.stream().filter(row -> row.device().deviceName().equals(name)).findFirst().orElseThrow();
    }
}
