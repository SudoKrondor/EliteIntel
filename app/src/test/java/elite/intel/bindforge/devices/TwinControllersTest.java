package elite.intel.bindforge.devices;

import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.devices.model.Device;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Two controllers on one VID/PID, which the game cannot tell apart. The GUIDs were reported by real controllers: the
 * VKB pair is Krondor's two Gladiator EVO R sticks (2026-11-22), the throttle sits on the same VID with its own PID.
 */
class TwinControllersTest {

    private static final String VKB_GLADIATOR_GUID = "0300fca31d2300000002000011010000";  // 231D:0200
    private static final String VKB_THROTTLE_GUID = "0300b1361d2300002c01000011010000";   // 231D:012C

    @Test
    void twoSticksOfOneModelAreFound() {
        List<TwinControllers.Twin> twins = TwinControllers.of(List.of(
                device(10, "VKBsim Gladiator EVO R", VKB_GLADIATOR_GUID, "usb-1"),
                device(11, "VKBsim Gladiator EVO R", VKB_GLADIATOR_GUID, "usb-5"),
                device(12, "S-TECS MODERN THROTTLE MINI PLUS", VKB_THROTTLE_GUID, "usb-2")));

        assertEquals(1, twins.size());
        TwinControllers.Twin twin = twins.getFirst();
        assertEquals(new HardwareId("231D", "0200"), twin.hardware());
        assertEquals("VKBsim Gladiator EVO R", twin.hardwareName());
        assertEquals(2, twin.count());
    }

    /** The same maker's other controller shares the VID only, which the game tells apart fine. */
    @Test
    void sharingOnlyTheVendorIsNotATwin() {
        assertTrue(TwinControllers.of(List.of(
                device(10, "VKBsim Gladiator EVO R", VKB_GLADIATOR_GUID, "usb-1"),
                device(12, "S-TECS MODERN THROTTLE MINI PLUS", VKB_THROTTLE_GUID, "usb-2"))).isEmpty());
    }

    @Test
    void controllersWithNoReadableIdAreLeftOut() {
        assertTrue(TwinControllers.of(List.of(
                device(10, "Mystery", "not-a-guid", "usb-1"),
                device(11, "Mystery", "not-a-guid", "usb-2"))).isEmpty());
    }

    private static Device device(int id, String name, String guid, String usbPath) {
        return new Device(id, name, 6, 32, usbPath, guid, false);
    }
}
