package elite.intel.bindforge.devices;

import elite.intel.bindforge.devicefiles.DeviceEntry;
import elite.intel.devices.model.Device;
import elite.intel.devices.model.DeviceIdentity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Turns a connected controller into the VID/PID identity the game's files are written in terms of.
 * <p>
 * {@code DeviceService} reports a {@link Device} carrying SDL's 32-character GUID, and never a VID or PID: it
 * reads them from SDL for its own duplicate check, into a private map keyed by a session-scoped id that is
 * not published. So a consumer derives them from the GUID, which is what {@code PACKAGE.md} says to do -
 * {@code DeviceIdentity} is documented there as "not produced by DeviceService directly, derived by consumers
 * from a Device".
 * <p>
 * It lives in BindForge rather than beside {@code DeviceIdentity} because BindForge is the only consumer that
 * needs it today, and {@code elite.intel.devices} is shared infrastructure being shipped to weekly. If a
 * second consumer appears, this moves.
 *
 * @see <a href="file:../../devices/PACKAGE.md">elite.intel.devices PACKAGE.md</a>
 */
public final class DeviceIdentities {

    private static final Logger log = LogManager.getLogger(DeviceIdentities.class);

    /** SDL writes a joystick GUID as 16 bytes, rendered as 32 hex characters. */
    private static final int GUID_HEX_LENGTH = 32;

    /**
     * Where the vendor and product sit inside that GUID, as offsets into the hex string.
     * <p>
     * SDL lays a joystick GUID out as bus type, then a CRC of the name, then vendor, a zero pair, product,
     * another zero pair, version, and two driver bytes - each 16-bit field little-endian. That puts vendor at
     * byte 4 and product at byte 8, which is hex offset 8 and hex offset 16.
     * <p>
     * <strong>Confirmed against real hardware 2026-10-02</strong>, on four controllers read at once, each
     * against a source other than this code - the VIRPIL sticks against a USB-level capture, the T-Rudder
     * against Frontier's own shipped entry, vJoy against its published ids. See {@code DeviceIdentitiesTest},
     * which carries their real GUIDs.
     * <p>
     * {@code PACKAGE.md} says "bytes 8-11", which turns out to be hex characters mislabelled as bytes: those
     * four characters are the vendor field, and it does not say where the product is. The reading above is
     * what the hardware actually produces.
     */
    private static final int VENDOR_HEX_OFFSET = 8;
    private static final int PRODUCT_HEX_OFFSET = 16;

    /**
     * GUIDs already logged, so a controller is described once per run rather than on every refresh.
     * <p>
     * The device list is rebuilt whenever the screen refreshes - which includes every ship-profile change -
     * and a line per controller per refresh would bury the log.
     */
    private static final Set<String> DESCRIBED = ConcurrentHashMap.newKeySet();

    private DeviceIdentities() {
    }

    /**
     * The VID/PID identity of a connected controller.
     *
     * @return the identity, or empty when the device reported no usable GUID - {@code readGuid} returns an
     *         empty string on failure, and a device we cannot identify is a real state rather than an error
     */
    public static Optional<DeviceIdentity> of(Device device) {
        String guid = device.guid();
        if (guid == null || guid.length() < GUID_HEX_LENGTH) {
            log.debug("Controller '{}' reported no usable GUID, so it cannot be matched to a device entry",
                    device.name());
            return Optional.empty();
        }

        String vid = littleEndianField(guid, VENDOR_HEX_OFFSET);
        String pid = littleEndianField(guid, PRODUCT_HEX_OFFSET);
        DeviceIdentity identity = new DeviceIdentity(vid, pid, vid + pid, device.usbPath());
        describeOnce(device, guid, identity);
        return Optional.of(identity);
    }

    /**
     * Reads one 16-bit field out of the GUID and swaps its bytes.
     * <p>
     * SDL writes each field little-endian, so the hex reads low byte first: a vendor of {@code 3344} appears
     * as {@code 4433}. Returned uppercase, matching {@link DeviceEntry.HardwareId}, because Frontier's own
     * file mixes cases and every comparison folds them.
     */
    private static String littleEndianField(String guid, int hexOffset) {
        String low = guid.substring(hexOffset, hexOffset + 2);
        String high = guid.substring(hexOffset + 2, hexOffset + 4);
        return (high + low).toUpperCase();
    }

    /**
     * Logs what was made of a controller, once per GUID per run.
     * <p>
     * Deliberately at INFO rather than DEBUG. A wrong reading here is silent and total - every controller
     * fails to match its entry, the device list shows known sticks as unrecognised hardware, and nothing
     * errors - so the one line that would reveal it has to be visible without turning logging up.
     */
    private static void describeOnce(Device device, String guid, DeviceIdentity identity) {
        if (!DESCRIBED.add(guid)) return;
        log.info("Controller '{}' reports GUID {} - read as VID {} PID {} (binds id {})",
                device.name(), guid, identity.vid(), identity.pid(), identity.bindsHexId());
    }
}
