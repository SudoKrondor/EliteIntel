package elite.intel.bindforge.devices;

import elite.intel.bindforge.devicefiles.DeviceEntry;

/**
 * One row of the My Devices list: a controller, whether BindForge learned of it from the hardware or from a
 * file.
 *
 * @param hardwareName what the hardware reports through the Device Service, or {@code null} when it is not
 *                     attached - a row built from a file entry alone has no hardware name, because nothing is
 *                     there to report one
 * @param entryName   the element tag naming this device in the working copy's {@code DeviceMappings.xml} - or, with
 *                    none there, the edits' - or {@code null}. Distinct from {@code alias}: this is what the last
 *                    write left, while the alias is the name with the edits on screen
 * @param vid         vendor id, uppercase hex
 * @param pid         product id, uppercase hex
 * @param attached    whether the controller is plugged in right now. A row is never dropped for being
 *                    unattached - entries deliberately outlive hardware, which is what makes a stick work
 *                    again when it is plugged back in
 * @param alias       the name an element of the user's gives this device in the edits, or {@code null} when none
 *                    does. Shown as <em>not added</em>
 * @param builtIn     whether one of Frontier's own entries already covers this hardware and the device is still under
 *                    Frontier's name, in which case the game already names it and there is nothing for BindForge to
 *                    create. A built-in an element of the user's names - a shadow (A15) - is the user's device, and
 *                    is not
 */
public record MyDevice(
        String hardwareName,
        String entryName,
        String vid,
        String pid,
        boolean attached,
        String alias,
        boolean builtIn) {

    /** The hardware this row describes, for correlating it against entries. */
    public DeviceEntry.HardwareId hardware() {
        return new DeviceEntry.HardwareId(vid, pid);
    }

    /**
     * The best human label available, falling back through what is known.
     * <p>
     * A row built from a file entry for hardware that is not plugged in has no hardware name at all, and
     * before first setup it has no alias either - so without this it would be a line of hex. The entry name
     * is the only thing naming it, and it is a real name a human chose.
     */
    public String label() {
        if (hardwareName != null) return hardwareName;
        if (entryName != null) return entryName;
        return vid + ":" + pid;
    }
}
