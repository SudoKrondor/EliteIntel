package elite.intel.bindforge.devices;

import elite.intel.bindforge.devicefiles.FrontierStockDevices;
import elite.intel.devices.model.Device;
import elite.intel.devices.model.DeviceIdentity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reading a controller's VID/PID out of SDL's GUID.
 * <p>
 * <strong>Every GUID here was reported by real hardware</strong>, captured 2026-10-02 from four controllers
 * attached at once, and every expected VID/PID is known from a source other than this code: the VIRPIL sticks
 * from a USB-level capture ({@code docs/USBTreeViewOfVirpilControllers.txt}), the T-Rudder from Frontier's own
 * shipped device list, and vJoy from its published ids.
 * <p>
 * That independence is the point. A GUID invented to match the layout under test only proves the code agrees
 * with itself, and getting these offsets wrong fails silently - every controller would simply never match its
 * entry, with nothing erroring anywhere.
 */
class DeviceIdentitiesTest {

    /**
     * The layout these samples confirm: bus type, a CRC of the device name, vendor, a zero pair, product,
     * then zeroes - each 16-bit field written low byte first.
     * <pre>
     * 0300 2cec 4433 0000 f483 0000 0000 0000
     * bus  crc  vid  --   pid  --
     * </pre>
     * The CRC is what rules out any reading that puts the vendor earlier: it differs across all four
     * controllers while the vendor and product fields line up exactly where expected.
     */
    private static final String VIRPIL_LEFT = "03002cec44330000f483000000000000";
    private static final String VIRPIL_RIGHT = "03009ba344330000f503000000000000";
    private static final String T_RUDDER = "030034554f04000079b6000000000000";
    private static final String VJOY = "030088bd34120000adbe000000000000";

    /** Matches the USB capture: VID 0x3344 (VIRPIL, UAB), PID 0x83F4. */
    @Test
    void theLeftVirpilStickReadsAsItsUsbIds() {
        DeviceIdentity identity = identityOf(VIRPIL_LEFT);

        assertEquals("3344", identity.vid());
        assertEquals("83F4", identity.pid());
    }

    /** The same capture's second device: VID 0x3344, PID 0x03F5. */
    @Test
    void theRightVirpilStickReadsAsItsUsbIds() {
        DeviceIdentity identity = identityOf(VIRPIL_RIGHT);

        assertEquals("3344", identity.vid());
        assertEquals("03F5", identity.pid(), "the leading zero is part of it, not formatting");
    }

    /**
     * Confirmed against a source neither SDL nor the USB capture: Frontier ships {@code <T-Rudder>} at
     * {@code 044F:B679}, so this reading is what makes the game recognise the stick as a built-in.
     */
    @Test
    void theTRudderReadsAsFrontiersOwnEntryForIt() {
        DeviceIdentity identity = identityOf(T_RUDDER);

        assertEquals("044F", identity.vid());
        assertEquals("B679", identity.pid());
        assertEquals("T-Rudder", FrontierStockDevices.getInstance().covering("044F", "B679").orElseThrow().name(),
                "and the stock reference agrees, which is what marks it BUILT-IN");
    }

    /** A virtual device identifies itself the same way a physical one does - vJoy's published 1234:BEAD. */
    @Test
    void aVirtualControllerIsReadNoDifferently() {
        DeviceIdentity identity = identityOf(VJOY);

        assertEquals("1234", identity.vid());
        assertEquals("BEAD", identity.pid());
    }

    /**
     * {@code .binds} names a device with no entry by VID and PID concatenated, so this string is what a
     * {@code Device=} attribute is matched against.
     */
    @Test
    void theBindsIdIsTheTwoHalvesJoined() {
        assertEquals("334483F4", identityOf(VIRPIL_LEFT).bindsHexId());
    }

    /**
     * SDL writes this hex lowercase. Frontier's own file mixes cases within a single line, so everything
     * downstream folds case - producing one case here means the folding has nothing to undo.
     */
    @Test
    void theHexComesBackUppercaseThoughSdlWroteItLower() {
        assertEquals("83F4", identityOf(VIRPIL_LEFT).pid());
        assertEquals("83F4", identityOf(VIRPIL_LEFT.toUpperCase()).pid(), "and uppercase input is no different");
    }

    @Test
    void theUsbPathIsCarriedThrough() {
        assertEquals("usb-0001", identityOf(VIRPIL_LEFT).usbPath());
    }

    /**
     * {@code readGuid} returns an empty string when SDL cannot supply one. A controller that cannot be
     * identified is a real state - it simply cannot be matched to a device entry - rather than a failure.
     */
    @Test
    void aDeviceWithNoGuidHasNoIdentity() {
        assertTrue(DeviceIdentities.of(deviceWith("")).isEmpty());
        assertTrue(DeviceIdentities.of(deviceWith("03000000")).isEmpty(), "and a truncated one is no better");
    }

    private static DeviceIdentity identityOf(String guid) {
        return DeviceIdentities.of(deviceWith(guid)).orElseThrow();
    }

    private static Device deviceWith(String guid) {
        return new Device(1, "VIRPIL Controls CONF_A02 L-L-VP-WBD-CAP", 6, 32, "usb-0001", guid);
    }
}
