package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Target;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * First setup reads the user's elements and the labels worth mastering, and sets Frontier's aside.
 */
class FirstSetupReaderTest {

    private static final String DEVICE_MAPPINGS = """
            <?xml version="1.0" encoding="UTF-8" ?>
            <Root>
            \t<LVWAP><PID>83f4</PID><VID>3344</VID></LVWAP>
            \t<T-Rudder><PID>B679</PID><VID>044F</VID></T-Rudder>
            \t<VPCPanel><PID>0259</PID><VID>3344</VID></VPCPanel>
            \t<GamePad><PID>028E</PID><VID>045E</VID></GamePad>
            </Root>
            """;

    @TempDir
    Path temp;

    @Test
    void theUsersElementsAreReadAndFrontiersAreSetAside() throws IOException {
        Target target = installation();

        FirstSetupPlan.Installation read = read(target);

        assertEquals(Map.of("LVWAP", new HardwareId("3344", "83F4")), read.elements());
        assertEquals(1, read.installId());
        assertEquals("STEAM", read.storefront());
    }

    @Test
    void labelsBesideABuiltInAreTakenButFrontiersOwnMapIsNot() throws IOException {
        Target target = installation();
        buttonMap(target, "T-Rudder", "<Joy_XAxis>X Axis</Joy_XAxis>");
        buttonMap(target, "LVWAP", "<Joy_1>TRIGGER</Joy_1>");
        buttonMap(target, "VPCPanel", "<Joy_1>B1</Joy_1>");

        FirstSetupPlan.Installation read = read(new FrontierStockButtonMaps(
                Map.of("VPCPanel", Map.of("Joy_1", "B1"))), target);

        assertEquals(Map.of("T-Rudder", new HardwareId("044F", "B679")), read.builtIns());
        assertEquals(Map.of("LVWAP", Map.of("Joy_1", "TRIGGER"), "T-Rudder", Map.of("Joy_XAxis", "X Axis")),
                read.labels(), "VPCPanel.buttonMap is Frontier's, and never mastered");
    }

    @Test
    void frontiersMapWithTheUsersChangesIsTheUsers() throws IOException {
        Target target = installation();
        buttonMap(target, "VPCPanel", "<Joy_1>MASTER ARM</Joy_1>");

        FirstSetupPlan.Installation read = read(new FrontierStockButtonMaps(
                Map.of("VPCPanel", Map.of("Joy_1", "B1"))), target);

        assertEquals(Map.of("VPCPanel", Map.of("Joy_1", "MASTER ARM")), read.labels());
        assertTrue(read.builtIns().containsKey("VPCPanel"));
    }

    @Test
    void theShippedMapsTravelInTheJarAndAreRecognisedByTheirLabels() throws IOException {
        FrontierStockButtonMaps shipped = FrontierStockButtonMaps.getInstance();
        for (String device : new String[]{"VPCPanel", "VPCThrottle"}) {
            Map<String, String> labels;
            String resource = "/bindforge/FrontierStock-" + device + ".buttonMap.xml";
            try (var in = getClass().getResourceAsStream(resource)) {
                assertNotNull(in, resource + " is not in the jar");
                labels = ButtonMapReader.read(in.readAllBytes());
            }
            assertTrue(shipped.isShipped(device, labels), device);
            Map<String, String> changed = new java.util.HashMap<>(labels);
            changed.put("Joy_1", "MASTER ARM");
            assertFalse(shipped.isShipped(device, changed), device + " with the user's label is the user's");
        }
        assertFalse(shipped.isShipped("T-Rudder", Map.of()), "Frontier ships no T-Rudder map");
    }

    @Test
    void anInstallationWithNoDeviceMappingsHoldsNothing() throws IOException {
        Path controlSchemes = Files.createDirectories(temp.resolve("ControlSchemes"));

        FirstSetupPlan.Installation read = read(new Target(1, "STEAM", controlSchemes, false));

        assertTrue(read.elements().isEmpty());
    }

    /** An unread file could hold the one entry the master must not lose, so first setup does not guess. */
    @Test
    void anUnreadableButtonMapStopsTheReading() throws IOException {
        Target target = installation();
        Path broken = target.controlSchemes().resolve("DeviceButtonMaps").resolve("LVWAP.buttonMap");
        Files.createDirectories(broken.getParent());
        Files.writeString(broken, "<Root><Joy_1>", StandardCharsets.UTF_8);

        IOException thrown = assertThrows(IOException.class, () -> read(target));
        assertTrue(thrown.getMessage().contains("LVWAP.buttonMap"));
    }

    private FirstSetupPlan.Installation read(Target target) throws IOException {
        return read(new FrontierStockButtonMaps(Map.of()), target);
    }

    private FirstSetupPlan.Installation read(FrontierStockButtonMaps maps, Target target) throws IOException {
        return FirstSetupReader.read(target, FrontierStockDevices.getInstance(), maps);
    }

    private Target installation() throws IOException {
        Path controlSchemes = Files.createDirectories(temp.resolve("ControlSchemes"));
        Files.writeString(controlSchemes.resolve("DeviceMappings.xml"), DEVICE_MAPPINGS, StandardCharsets.UTF_8);
        return new Target(1, "STEAM", controlSchemes, false);
    }

    private static void buttonMap(Target target, String device, String body) throws IOException {
        Path file = target.controlSchemes().resolve("DeviceButtonMaps").resolve(device + ".buttonMap");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<Root>\n" + body + "\n</Root>\n",
                StandardCharsets.UTF_8);
    }
}
