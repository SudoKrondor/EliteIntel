package elite.intel.devices.hid;

/**
 * One input a controller's HID description declares: its usage page and usage id - Generic Desktop {@code 0x35}
 * is Rz, Simulation {@code 0xBB} is Throttle.
 */
public record HidUsage(int page, int id) {

    public static final int GENERIC_DESKTOP = 0x01;
    public static final int SIMULATION = 0x02;
}
