package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceFilesPush.Target;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What settles a removal or a rename after catch-up: whether any installation may still hold the name, read from the
 * files as the push would find it (Alan, 2026-10-15, A14).
 */
class InstallationHoldingsTest {

    private static final String WITH_LVWAP = """
            <?xml version="1.0" encoding="UTF-8" ?>
            <Root>
            \t<LVWAP><VID>3344</VID><PID>83F4</PID></LVWAP>
            \t<T-Rudder><PID>B679</PID><VID>044F</VID></T-Rudder>
            </Root>
            """;
    private static final String FRONTIERS_ONLY = """
            <?xml version="1.0" encoding="UTF-8" ?>
            <Root>
            \t<T-Rudder><PID>B679</PID><VID>044F</VID></T-Rudder>
            </Root>
            """;

    @TempDir
    Path temp;

    @Test
    void aUserNameIsHeldByItsElement() throws IOException {
        Target steam = installation(1, WITH_LVWAP);

        assertTrue(holdings(steam).mayHoldRemoval("LVWAP"));
    }

    @Test
    void aUserNameIsHeldByItsButtonMapAlone() throws IOException {
        Target steam = installation(1, FRONTIERS_ONLY);
        buttonMap(steam, "LVWAP");

        assertTrue(holdings(steam).mayHoldRemoval("LVWAP"));
    }

    @Test
    void aNameNoInstallationHoldsIsSettled() throws IOException {
        Target steam = installation(1, FRONTIERS_ONLY);
        Target epic = installation(2, FRONTIERS_ONLY);

        assertFalse(holdings(steam, epic).mayHoldRemoval("LVWAP"));
    }

    /** One of Frontier's names keeps its element - only a map BindForge or the user made counts. */
    @Test
    void aFrontierNameIsHeldOnlyByAMapThatIsNotFrontiers() throws IOException {
        Target steam = installation(1, FRONTIERS_ONLY);
        assertFalse(holdings(steam).mayHoldRemoval("T-Rudder"), "Frontier's element alone");

        buttonMap(steam, "T-Rudder");
        assertTrue(holdings(steam).mayHoldRemoval("T-Rudder"));
    }

    @Test
    void aShippedFrontierMapIsNotHeld() throws IOException {
        Target steam = installation(1, FRONTIERS_ONLY);
        byte[] shipped = "<Root>Frontier</Root>".getBytes();
        Files.createDirectories(steam.controlSchemes().resolve("DeviceButtonMaps"));
        Files.write(steam.controlSchemes().resolve("DeviceButtonMaps").resolve("VPCPanel.buttonMap"), shipped);
        FrontierStockButtonMaps stockMaps = new FrontierStockButtonMaps(Map.of(), Map.of("VPCPanel", shipped));

        assertFalse(new InstallationHoldings(() -> List.of(steam), FrontierStockDevices.getInstance(), stockMaps)
                .mayHoldRemoval("VPCPanel"));
    }

    /** Out of reach and missed a write: it may hold anything, so nothing is settled. */
    @Test
    void anUnreachableInstallationThatMissedAWriteMayHoldIt() throws IOException {
        Target steam = installation(1, FRONTIERS_ONLY);
        Target away = new Target(2, "EPIC", temp.resolve("gone"), false, true);

        assertTrue(holdings(steam, away).mayHoldRemoval("LVWAP"));
        assertTrue(holdings(steam, away).mayHoldOldName("LVWAP", "LeftStick", "3344", "83F4"));
    }

    /** Out of reach but missed nothing: it took every write that carried everything, so it settles nothing either way. */
    @Test
    void anUnreachableInstallationThatMissedNothingDoesNotHoldItBack() throws IOException {
        Target steam = installation(1, FRONTIERS_ONLY);
        Target away = new Target(2, "EPIC", temp.resolve("gone"), true, false);

        assertFalse(holdings(steam, away).mayHoldRemoval("LVWAP"));
    }

    @Test
    void anUnreadableFileMayHoldIt() throws IOException {
        Target steam = installation(1, "<Root><broken");

        assertTrue(holdings(steam).mayHoldRemoval("LVWAP"));
    }

    @Test
    void anOldNameIsHeldByItsElementIgnoringCase() throws IOException {
        Target steam = installation(1, WITH_LVWAP.replace("LVWAP", "lvwap"));

        assertTrue(holdings(steam).mayHoldOldName("LVWAP", "LeftStick", "3344", "83F4"));
    }

    @Test
    void anOldNameIsHeldByItsButtonMap() throws IOException {
        Target steam = installation(1, FRONTIERS_ONLY);
        buttonMap(steam, "LVWAP");

        assertTrue(holdings(steam).mayHoldOldName("LVWAP", "LeftStick", "3344", "83F4"));
    }

    /** A case-only rename: the element spelled exactly as the new name is the device itself, never an old copy. */
    @Test
    void aCaseOnlyRenameIsNotHeldByItsNewSpelling() throws IOException {
        Target steam = installation(1, WITH_LVWAP.replace("LVWAP", "Lvwap"));

        assertFalse(holdings(steam).mayHoldOldName("LVWAP", "Lvwap", "3344", "83F4"));
    }

    @Test
    void aRenameEveryInstallationTookIsSettled() throws IOException {
        Target steam = installation(1, WITH_LVWAP.replace("LVWAP", "LeftStick"));
        buttonMap(steam, "LeftStick");

        assertFalse(holdings(steam).mayHoldOldName("LVWAP", "LeftStick", "3344", "83F4"));
    }

    // --- a shadow's old name is Frontier's, whose element stays (A15)

    private static final String SHADOWED = """
            <?xml version="1.0" encoding="UTF-8" ?>
            <Root>
            \t<MyRudder><PID>B679</PID><VID>044F</VID></MyRudder>
            \t<T-Rudder><PID>B679</PID><VID>044F</VID></T-Rudder>
            </Root>
            """;

    @Test
    void aShadowLeadingTheFileWithNoOldMapIsSettled() throws IOException {
        Target steam = installation(1, SHADOWED);

        assertFalse(holdings(steam).mayHoldOldName("T-Rudder", "MyRudder", "044f", "b679"),
                "Frontier's element staying below the user's does not hold the old name");
    }

    @Test
    void aShadowIsHeldWhileFrontiersElementStillLeads() throws IOException {
        Target steam = installation(1, FRONTIERS_ONLY);

        assertTrue(holdings(steam).mayHoldOldName("T-Rudder", "MyRudder", "044F", "B679"));
    }

    @Test
    void aShadowIsHeldWhileTheOldMapIsStillThere() throws IOException {
        Target steam = installation(1, SHADOWED);
        buttonMap(steam, "T-Rudder");

        assertTrue(holdings(steam).mayHoldOldName("T-Rudder", "MyRudder", "044F", "B679"));
    }

    @Test
    void aShadowedFrontierMapPutBackAsShippedIsSettled() throws IOException {
        Target steam = installation(1, SHADOWED.replace("T-Rudder", "VPCPanel"));
        byte[] shipped = "<Root>Frontier</Root>".getBytes();
        Files.createDirectories(steam.controlSchemes().resolve("DeviceButtonMaps"));
        Files.write(steam.controlSchemes().resolve("DeviceButtonMaps").resolve("VPCPanel.buttonMap"), shipped);
        FrontierStockButtonMaps stockMaps = new FrontierStockButtonMaps(Map.of(), Map.of("VPCPanel", shipped));

        assertFalse(new InstallationHoldings(() -> List.of(steam), FrontierStockDevices.getInstance(), stockMaps)
                .mayHoldOldName("VPCPanel", "MyRudder", "044F", "B679"));
    }

    @Test
    void aShadowIsHeldWhereTheFileIsGone() throws IOException {
        Path controlSchemes = Files.createDirectories(temp.resolve("bare").resolve("ControlSchemes"));
        Target steam = new Target(1, "STEAM1", controlSchemes, false);

        assertTrue(holdings(steam).mayHoldOldName("T-Rudder", "MyRudder", "044F", "B679"));
    }

    private InstallationHoldings holdings(Target... targets) {
        return new InstallationHoldings(() -> List.of(targets), FrontierStockDevices.getInstance(),
                FrontierStockButtonMaps.getInstance());
    }

    private Target installation(long id, String deviceMappings) throws IOException {
        Path controlSchemes = Files.createDirectories(temp.resolve("install" + id).resolve("ControlSchemes"));
        Files.writeString(controlSchemes.resolve("DeviceMappings.xml"), deviceMappings);
        return new Target(id, "STEAM" + id, controlSchemes, false);
    }

    private static void buttonMap(Target target, String device) throws IOException {
        Path folder = Files.createDirectories(target.controlSchemes().resolve("DeviceButtonMaps"));
        Files.writeString(folder.resolve(device + ".buttonMap"), "<Root/>");
    }
}
