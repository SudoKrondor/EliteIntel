package elite.intel.bindforge.devices;

import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devicefiles.FrontierStockDevices;
import elite.intel.devices.model.Device;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Built against the shipped reference itself, so the list is checked on Frontier's real shapes - an entry with
 * eighty pairs, and one matching two controllers through its alternatives.
 */
class BuiltInDeviceListTest {

    private static final String T_RUDDER_GUID = "030034554f04000079b6000000000000";   // 044F:B679, real hardware
    /** 054C:05C4, the DualShock 4 v1 - laid out as SDL lays out a GUID, not captured from the controller. */
    private static final String DUALSHOCK4_V1_GUID = "030000004c050000c405000000000000";
    private static final String VIRPIL_LEFT_GUID = "03002cec44330000f483000000000000"; // 3344:83F4, not Frontier's

    private final FrontierStockDevices stock = FrontierStockDevices.getInstance();

    @Test
    void everyFrontierEntryIsARowInFileOrder() {
        List<BuiltInDevice> rows = BuiltInDeviceList.build(stock, List.of());

        assertEquals(51, rows.size());
        assertEquals(stock.names(), rows.stream().map(BuiltInDevice::name).toList());
        assertEquals(136, rows.stream().mapToInt(row -> 1 + row.alternatives().size()).sum(),
                "every pair is either a primary or an alternative");
    }

    @Test
    void theElementsOwnPairIsThePrimaryAndTheRestAreAlternatives() {
        Map<String, BuiltInDevice> rows = byName(BuiltInDeviceList.build(stock, List.of()));

        BuiltInDevice gamePad = rows.get("GamePad");
        assertEquals(new HardwareId("045E", "028E"), gamePad.primary());
        assertEquals(79, gamePad.alternatives().size());

        BuiltInDevice dualShock = rows.get("DualShock4");
        assertEquals(new HardwareId("054C", "0BA0"), dualShock.primary(), "the USB adaptor, not either controller");
        assertEquals(List.of(new HardwareId("054C", "05C4"), new HardwareId("054C", "09CC")), dualShock.alternatives());

        assertTrue(rows.get("T-Rudder").alternatives().isEmpty());
    }

    @Test
    void nothingIsAttachedWhenNoControllerIs() {
        assertTrue(BuiltInDeviceList.build(stock, List.of()).stream().noneMatch(BuiltInDevice::attached));
    }

    @Test
    void anAttachedControllerMarksTheEntryItResolvesTo() {
        Map<String, BuiltInDevice> rows = byName(BuiltInDeviceList.build(stock,
                List.of(device("T.Flight Rudder Pedals", T_RUDDER_GUID))));

        assertTrue(rows.get("T-Rudder").attached());
        assertEquals(1, rows.values().stream().filter(BuiltInDevice::attached).count());
    }

    /** A controller matching only an {@code <Alternative>} is still that entry's - the DualShock 4 is never the primary. */
    @Test
    void aControllerMatchingAnAlternativeMarksItsEntry() {
        Map<String, BuiltInDevice> rows = byName(BuiltInDeviceList.build(stock,
                List.of(device("Wireless Controller", DUALSHOCK4_V1_GUID))));

        assertTrue(rows.get("DualShock4").attached());
    }

    @Test
    void aControllerFrontierDoesNotShipMarksNothing() {
        List<BuiltInDevice> rows = BuiltInDeviceList.build(stock,
                List.of(device("VIRPIL Controls L-VP-WBD-CAP", VIRPIL_LEFT_GUID)));

        assertFalse(rows.stream().anyMatch(BuiltInDevice::attached));
    }

    private static Device device(String name, String guid) {
        return new Device(1, name, 6, 32, "usb-0001", guid);
    }

    private static Map<String, BuiltInDevice> byName(List<BuiltInDevice> rows) {
        return rows.stream().collect(Collectors.toMap(BuiltInDevice::name, Function.identity()));
    }
}
