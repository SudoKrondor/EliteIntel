package elite.intel.bindforge.devices;

/**
 * One row of the My Devices list: a controller, whether BindForge learned of it from the hardware or from a
 * file.
 *
 * @param windowsName what the hardware reports through the Device Service, or {@code null} when it is not
 *                    attached - a row built from a file entry alone has no Windows name, because nothing is
 *                    there to report one
 * @param entryName   the element tag naming this device in an installation's {@code DeviceMappings.xml}, or
 *                    {@code null} when no installation has an entry for it. Distinct from {@code alias}: this
 *                    is what is on disk now, while the alias is what the user chose
 * @param vid         vendor id, uppercase hex
 * @param pid         product id, uppercase hex
 * @param attached    whether the controller is plugged in right now. A row is never dropped for being
 *                    unattached - entries deliberately outlive hardware, which is what makes a stick work
 *                    again when it is plugged back in
 * @param alias       the name this device has in the master, or {@code null} when the master has no record of
 *                    it. Shown as <em>not added</em>
 * @param builtIn     whether one of Frontier's own entries already covers this hardware, in which case the
 *                    game already names it and there is nothing for BindForge to create
 */
public record MyDevice(
        String windowsName,
        String entryName,
        String vid,
        String pid,
        boolean attached,
        String alias,
        boolean builtIn) {

    /** The hardware this row describes, for correlating it against entries and master records. */
    public DeviceEntry.HardwareId hardware() {
        return new DeviceEntry.HardwareId(vid, pid);
    }

    /**
     * The best human label available, falling back through what is known.
     * <p>
     * A row built from a file entry for hardware that is not plugged in has no Windows name at all, and
     * before first setup it has no alias either - so without this it would be a line of hex. The entry name
     * is the only thing naming it, and it is a real name a human chose.
     */
    public String label() {
        if (windowsName != null) return windowsName;
        if (entryName != null) return entryName;
        return vid + ":" + pid;
    }
}
