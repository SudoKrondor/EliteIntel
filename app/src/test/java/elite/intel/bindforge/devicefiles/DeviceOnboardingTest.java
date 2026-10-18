package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Target;
import elite.intel.bindforge.devicefiles.DeviceOnboarding.InstallationResult;
import elite.intel.bindforge.devicefiles.DeviceOnboarding.Naming;
import elite.intel.bindforge.devicefiles.DeviceOnboarding.Outcome;
import elite.intel.db.dao.BindForgeHardwareIdMoveDao.MoveRow;
import elite.intel.db.managers.BindForgeDeviceDraftManager.StoredDevice;
import elite.intel.io.TimestampedBackups;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Onboarding's writes (A6b, 2026-10-14): a controller no binding names by hex is written to every installation
 * straight away, each installation all or nothing; one {@code .binds} names by hex is only stored, for Apply.
 */
class DeviceOnboardingTest {

    private static final String DEVICE_MAPPINGS = """
            <?xml version="1.0" encoding="UTF-8" ?>
            <Root>
            \t<T-Rudder><PID>B679</PID><VID>044F</VID></T-Rudder>
            </Root>
            """;

    private static final Naming STICK =
            new Naming("VirpilControls20220720", "3344", "0194", Map.of("Joy_1", "Button 1"), 0);
    private static final Naming HEX_STICK = new Naming("LeftStick", "3344", "83F3", Map.of("Joy_1", "Button 1"), 131);

    @TempDir
    Path temp;

    private final FakeStore store = new FakeStore();

    @Test
    void aControllerNamedNowIsWrittenToEveryInstallation() throws IOException {
        Target steam = installation(1, "STEAM");
        Target epic = installation(2, "EPIC");

        DeviceOnboarding.Result result = onboarding(List.of(steam, epic)).name(List.of(STICK));

        assertTrue(result.recorded());
        assertEquals(2, result.matchingCount());
        for (Target target : List.of(steam, epic)) {
            String mappings = Files.readString(target.controlSchemes().resolve("DeviceMappings.xml"));
            assertTrue(mappings.indexOf("<VirpilControls20220720>") < mappings.indexOf("<T-Rudder>"),
                    "the user's element leads the file");
            assertTrue(Files.readString(buttonMap(target, "VirpilControls20220720")).contains("Button 1"));
        }
        assertEquals(List.of("VirpilControls20220720"), store.now);
        assertTrue(Files.isDirectory(temp.resolve("history").resolve("1")), "the replaced file is in Edit History");
    }

    /** An installation that cannot be reached is reported and left; the others are written (Alan, 2026-10-14). */
    @Test
    void anUnreachableInstallationIsReportedAndTheOthersWritten() throws IOException {
        Target steam = installation(1, "STEAM");
        Target unplugged = new Target(2, "EPIC", temp.resolve("unplugged").resolve("ControlSchemes"), false);

        DeviceOnboarding.Result result = onboarding(List.of(steam, unplugged)).name(List.of(STICK));

        assertEquals(1, result.matchingCount());
        assertEquals(Outcome.SKIPPED_MISSING, result.installations().get(1).outcome());
        assertTrue(Files.exists(buttonMap(steam, "VirpilControls20220720")));
    }

    /** A .buttonMap already under the name is the user's: kept, and said so. */
    @Test
    void aButtonMapAlreadyThereIsKept() throws IOException {
        Target steam = installation(1, "STEAM");
        Files.createDirectories(buttonMap(steam, "x").getParent());
        Files.writeString(buttonMap(steam, "VirpilControls20220720"), "theirs");

        InstallationResult result = onboarding(List.of(steam)).name(List.of(STICK)).installations().getFirst();

        assertEquals(Outcome.WRITTEN, result.outcome(), "the entry was still added");
        assertEquals(List.of("VirpilControls20220720"), result.keptButtonMaps());
        assertEquals("theirs", Files.readString(buttonMap(steam, "VirpilControls20220720")));
    }

    /** One installation is all or nothing: a file that cannot be read stops it before anything is written. */
    @Test
    void anUnreadableInstallationIsLeftAsItWas() throws IOException {
        Target steam = installation(1, "STEAM");
        Files.writeString(steam.controlSchemes().resolve("DeviceMappings.xml"), "<Root><broken>");

        InstallationResult result = onboarding(List.of(steam)).name(List.of(STICK)).installations().getFirst();

        assertEquals(Outcome.FAILED, result.outcome());
        assertFalse(Files.exists(buttonMap(steam, "VirpilControls20220720")));
        assertEquals("<Root><broken>", Files.readString(steam.controlSchemes().resolve("DeviceMappings.xml")));
    }

    /** A controller .binds names by hex is only stored - moving its bindings is Apply's. */
    @Test
    void aControllerNamedByHexWaitsForApplyAndWritesNoFile() throws IOException {
        Target steam = installation(1, "STEAM");

        DeviceOnboarding.Result result = onboarding(List.of(steam)).name(List.of(HEX_STICK));

        assertEquals(List.of(HEX_STICK), result.atApply());
        assertTrue(result.installations().isEmpty());
        assertEquals(List.of("LeftStick"), store.atApply);
        assertEquals(DEVICE_MAPPINGS, Files.readString(steam.controlSchemes().resolve("DeviceMappings.xml")));
    }

    /**
     * The identity question's yes for a controller known only by its old id (A17): the old id is stored with the name,
     * for Apply to move the bindings from it.
     */
    @Test
    void aControllerReplacingAnOldIdStoresItForApply() throws IOException {
        Target steam = installation(1, "STEAM");
        Naming replacing = new Naming("LeftStick", "3344", "83F4", Map.of("Joy_1", "Button 1"), 131,
                new HardwareId("3344", "83F3"));

        DeviceOnboarding.Result result = onboarding(List.of(steam)).name(List.of(replacing, STICK));

        assertEquals(List.of(replacing), result.atApply());
        assertEquals(List.of(new MoveRow("3344", "83F4", "3344", "83F3")), store.oldIds,
                "only the controller replacing an id");
    }

    @Test
    void nothingIsWrittenWhenTheNamesCannotBeStored() throws IOException {
        Target steam = installation(1, "STEAM");
        store.accepts = false;

        DeviceOnboarding.Result result = onboarding(List.of(steam)).name(List.of(STICK));

        assertFalse(result.recorded());
        assertFalse(Files.exists(buttonMap(steam, "VirpilControls20220720")));
    }

    /** No DeviceMappings.xml: seeded from Frontier's, as the push seeds it - without it the game loads no bindings. */
    @Test
    void anInstallationWithNoDeviceMappingsIsSeededFromTheStockFile() throws IOException {
        Target steam = installation(1, "STEAM");
        Files.delete(steam.controlSchemes().resolve("DeviceMappings.xml"));

        InstallationResult result = onboarding(List.of(steam)).name(List.of(STICK)).installations().getFirst();

        assertEquals(Outcome.WRITTEN, result.outcome());
        String mappings = Files.readString(steam.controlSchemes().resolve("DeviceMappings.xml"));
        assertTrue(mappings.contains("<VirpilControls20220720>"));
        assertTrue(mappings.contains("<SaitekX52>"), "Frontier's entries are there too");
    }

    /** A .buttonMap that cannot be written puts the installation back: DeviceMappings.xml as it was. */
    @Test
    void aFailedButtonMapWritePutsTheInstallationBack() throws IOException {
        Target steam = installation(1, "STEAM");
        Files.writeString(steam.controlSchemes().resolve("DeviceButtonMaps"), "a file where the folder should be");

        InstallationResult result = onboarding(List.of(steam)).name(List.of(STICK)).installations().getFirst();

        assertEquals(Outcome.FAILED, result.outcome());
        assertEquals(DEVICE_MAPPINGS, Files.readString(steam.controlSchemes().resolve("DeviceMappings.xml")));
    }

    /** One Edit History copy for one content, however many installations hold it - as the push keeps them. */
    @Test
    void identicalInstallationsKeepOneEditHistoryCopy() throws IOException {
        Target steam = installation(1, "STEAM");
        Target epic = installation(2, "EPIC");

        onboarding(List.of(steam, epic)).name(List.of(STICK));

        assertTrue(Files.isDirectory(temp.resolve("history").resolve("1")));
        assertFalse(Files.exists(temp.resolve("history").resolve("2")));
    }

    private DeviceOnboarding onboarding(List<Target> targets) {
        return new DeviceOnboarding(() -> targets, store,
                new EditHistory(new TimestampedBackups(), temp.resolve("history"), () -> 10), target -> { });
    }

    private Target installation(long id, String storefront) throws IOException {
        Path controlSchemes = temp.resolve(storefront).resolve("ControlSchemes");
        Files.createDirectories(controlSchemes);
        Files.writeString(controlSchemes.resolve("DeviceMappings.xml"), DEVICE_MAPPINGS);
        return new Target(id, storefront, controlSchemes, false);
    }

    private static Path buttonMap(Target target, String device) {
        return target.controlSchemes().resolve("DeviceButtonMaps").resolve(device + ".buttonMap");
    }

    /** The master and draft, as names only. */
    private static final class FakeStore implements DeviceOnboarding.Store {
        boolean accepts = true;
        final List<String> now = new ArrayList<>();
        final List<String> atApply = new ArrayList<>();
        final List<MoveRow> oldIds = new ArrayList<>();

        @Override
        public boolean onboard(List<StoredDevice> namedNow, List<StoredDevice> namedAtApply, List<MoveRow> moves) {
            if (!accepts) return false;
            namedNow.forEach(device -> now.add(device.deviceName()));
            namedAtApply.forEach(device -> atApply.add(device.deviceName()));
            oldIds.addAll(moves);
            return true;
        }

        @Override
        public List<String> masterOrder() {
            return List.copyOf(now);
        }
    }
}
