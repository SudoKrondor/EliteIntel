package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * First setup reads the user's elements and the labels worth taking into the working copy, and sets Frontier's aside.
 */
class FirstSetupReaderTest {

    private static final String DEVICE_MAPPINGS = """
            <?xml version="1.0" encoding="UTF-8" ?>
            <Root>
            	<LVWAP><PID>83f4</PID><VID>3344</VID></LVWAP>
            	<T-Rudder><PID>B679</PID><VID>044F</VID></T-Rudder>
            	<VPCPanel><PID>0259</PID><VID>3344</VID></VPCPanel>
            	<GamePad><PID>028E</PID><VID>045E</VID></GamePad>
            </Root>
            """;

    private final Map<String, byte[]> buttonMaps = new LinkedHashMap<>();

    @Test
    void theUsersElementsAreReadAndFrontiersAreSetAside() throws IOException {
        FirstSetupPlan.Installation read = read();

        assertEquals(Map.of("LVWAP", new HardwareId("3344", "83F4")), read.elements());
        assertEquals(1, read.installId());
        assertEquals("STEAM", read.storefront());
    }

    @Test
    void labelsBesideABuiltInAreTakenButFrontiersOwnMapIsNot() throws IOException {
        buttonMap("T-Rudder", "<Joy_XAxis>X Axis</Joy_XAxis>");
        buttonMap("LVWAP", "<Joy_1>TRIGGER</Joy_1>");
        buttonMap("VPCPanel", "<Joy_1>B1</Joy_1>");

        FirstSetupPlan.Installation read = read(new FrontierStockButtonMaps(
                Map.of("VPCPanel", Map.of("Joy_1", "B1"))));

        assertEquals(Map.of("T-Rudder", new HardwareId("044F", "B679")), read.builtIns());
        assertEquals(Map.of("LVWAP", Map.of("Joy_1", "TRIGGER"), "T-Rudder", Map.of("Joy_XAxis", "X Axis")),
                read.labels(), "VPCPanel.buttonMap is Frontier's, and never mastered");
    }

    @Test
    void frontiersMapWithTheUsersChangesIsTheUsers() throws IOException {
        buttonMap("VPCPanel", "<Joy_1>MASTER ARM</Joy_1>");

        FirstSetupPlan.Installation read = read(new FrontierStockButtonMaps(
                Map.of("VPCPanel", Map.of("Joy_1", "B1"))));

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
        FirstSetupPlan.Installation read = FirstSetupReader.read(1, "STEAM", null, Map.of(),
                FrontierStockDevices.getInstance(), new FrontierStockButtonMaps(Map.of()));

        assertTrue(read.elements().isEmpty());
    }

    /** An unread file could hold the one entry the working copy must not lose, so first setup does not guess. */
    @Test
    void anUnreadableButtonMapStopsTheReading() {
        buttonMaps.put("LVWAP.buttonMap", "<Root><Joy_1>".getBytes(StandardCharsets.UTF_8));

        IOException thrown = assertThrows(IOException.class, this::read);
        assertTrue(thrown.getMessage().contains("LVWAP.buttonMap"));
    }

    @Test
    void anUnreadableDeviceMappingsStopsTheReadingAndIsNamed() {
        IOException thrown = assertThrows(IOException.class, () -> FirstSetupReader.read(1, "STEAM",
                "<Root><LVWAP>".getBytes(StandardCharsets.UTF_8), Map.of(), FrontierStockDevices.getInstance(),
                new FrontierStockButtonMaps(Map.of())));
        assertTrue(thrown.getMessage().contains("DeviceMappings.xml"));
    }

    /** As Windows finds it: lvwap.buttonMap holds LVWAP's labels. */
    @Test
    void aButtonMapSpelledInAnotherCaseIsFound() throws IOException {
        buttonMaps.put("lvwap.buttonMap", map("<Joy_1>TRIGGER</Joy_1>"));

        assertEquals(Map.of("LVWAP", Map.of("Joy_1", "TRIGGER")), read().labels());
        assertEquals("lvwap.buttonMap", FirstSetupReader.buttonMapFileOf(buttonMaps, "LVWAP"));
        assertNull(FirstSetupReader.buttonMapFileOf(buttonMaps, "RVWAP"));
    }

    private FirstSetupPlan.Installation read() throws IOException {
        return read(new FrontierStockButtonMaps(Map.of()));
    }

    private FirstSetupPlan.Installation read(FrontierStockButtonMaps maps) throws IOException {
        return FirstSetupReader.read(1, "STEAM", DEVICE_MAPPINGS.getBytes(StandardCharsets.UTF_8), buttonMaps,
                FrontierStockDevices.getInstance(), maps);
    }

    private void buttonMap(String device, String body) {
        buttonMaps.put(device + ".buttonMap", map(body));
    }

    private static byte[] map(String body) {
        return ("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<Root>\n" + body + "\n</Root>\n")
                .getBytes(StandardCharsets.UTF_8);
    }
}
