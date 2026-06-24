package elite.intel.devices.model;

/**
 * A resolved device identity — VID, PID, and the hex string used in {@code .binds} files for
 * axis bindings.
 */
public record DeviceIdentity(
        String vid,           // vendor ID — 4 hex chars
        String pid,           // product ID — 4 hex chars
        String bindsHexId,    // VID+PID concatenated — matches Device= attribute in .binds axis bindings
        String usbPath        // USB port path for duplicate device differentiation
) {
}
