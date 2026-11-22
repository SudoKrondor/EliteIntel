package elite.intel.bindforge.devices;

import elite.intel.bindforge.devicefiles.DeviceEntry;
import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devicefiles.FrontierStockDevices;
import elite.intel.devices.model.Device;
import elite.intel.devices.model.DeviceIdentity;

import java.util.*;

/**
 * Builds the My Devices list: <strong>the union of live hardware and the working copy's
 * {@code DeviceMappings.xml}</strong>, correlated by VID/PID (alias-designer.md, <em>My Devices</em>).
 * <p>
 * The two halves answer different questions and neither is sufficient. {@code DeviceMappings.xml} supplies
 * <em>names</em> and never <em>existence</em> - the game writes nothing into it, so a controller only appears
 * there once somebody has named it. Build the list from the file alone and a brand-new stick is invisible
 * until the user edits a file by hand. Build it from the hardware alone and an entry for a controller sold
 * last year vanishes, taking with it the only sign that bindings still reference it.
 * <p>
 * <strong>The alias is the edits' name</strong> - the working copy with the edits on screen - so a name given or changed
 * shows at once, before APPLY (Storage S4a). The working copy's own name is the row's entry name, so a device cleared in
 * the edits is still listed, and still opens with its undo.
 * <p>
 * A pure function over its inputs, with no service or file access of its own. The rule it encodes is the one the whole
 * screen rests on, and it is worth being able to test without a UI or a game installation.
 */
public final class MyDeviceList {

    private MyDeviceList() {
    }

    /**
     * @param attached what the Device Service reports right now, in its own order
     * @param edited   every element in the edits' {@code DeviceMappings.xml}, Frontier's included - they are filtered
     *                 here rather than by the caller, so the rule lives in one place
     * @param applied  every element in the working copy's, the same way
     * @param stock    Frontier's shipped list, which decides what is a built-in
     * @return attached controllers first, in the order the Device Service gave them, then the user's entries for
     *         hardware that is not present
     */
    public static List<MyDevice> build(List<Device> attached,
                                       Collection<DeviceEntry> edited,
                                       Collection<DeviceEntry> applied,
                                       FrontierStockDevices stock) {
        Map<HardwareId, String> aliases = usersNamesByHardware(edited, stock);
        Map<HardwareId, String> appliedNames = usersNamesByHardware(applied, stock);

        Map<HardwareId, MyDevice> rows = new LinkedHashMap<>();

        for (Device device : attached) {
            Optional<DeviceIdentity> identity = DeviceIdentities.of(device);
            // WHY: a controller whose GUID could not be read is left out rather than added with blank ids.
            // Every column but its name is derived from the VID/PID, and a row keyed on nothing would merge
            // with the next such controller.
            if (identity.isEmpty()) continue;

            HardwareId hardware = new HardwareId(identity.get().vid(), identity.get().pid());
            rows.put(hardware, row(device.name(), hardware, true, aliases, appliedNames, stock));
        }

        Set<HardwareId> filed = new LinkedHashSet<>(aliases.keySet());
        filed.addAll(appliedNames.keySet());
        for (HardwareId hardware : filed) {
            // WHY: hardware already listed above is plugged in, and its row says so. Putting it again would move the
            // row to the end of the list.
            if (rows.containsKey(hardware)) continue;
            rows.put(hardware, row(null, hardware, false, aliases, appliedNames, stock));
        }

        return List.copyOf(rows.values());
    }

    private static MyDevice row(String hardwareName, HardwareId hardware, boolean attached,
                                Map<HardwareId, String> aliases, Map<HardwareId, String> appliedNames,
                                FrontierStockDevices stock) {
        String alias = aliases.get(hardware);
        String entryName = appliedNames.getOrDefault(hardware, alias);
        return new MyDevice(hardwareName, entryName, hardware.vid(), hardware.pid(), attached, alias,
                builtIn(hardware, alias, stock));
    }

    /**
     * Whether the row is a built-in: Frontier covers the hardware, and no element of the user's names it. <strong>A
     * shadow is the user's</strong> (Alan, 2026-10-16, A15) - an element of theirs leading the file for the controller.
     */
    private static boolean builtIn(HardwareId hardware, String alias, FrontierStockDevices stock) {
        return alias == null && stock.covering(hardware.vid(), hardware.pid()).isPresent();
    }

    /**
     * The name each piece of hardware has among the user's elements, Frontier's own left out.
     * <p>
     * Frontier's are dropped because they are not devices the user added - the game ships 51 of them, most for
     * hardware nobody owns, and listing them all under My Devices would bury the handful that are real. A built-in that
     * is actually <em>attached</em> still appears, because it comes in through the hardware half of the union, marked
     * {@code builtIn}. Two of the user's elements claiming one VID/PID: the first in the file, as the game picks.
     */
    private static Map<HardwareId, String> usersNamesByHardware(Collection<DeviceEntry> entries,
                                                                FrontierStockDevices stock) {
        Map<HardwareId, String> names = new LinkedHashMap<>();
        for (DeviceEntry entry : entries) {
            if (stock.ships(entry.name())) continue;
            for (HardwareId hardware : entry.hardware()) {
                names.putIfAbsent(hardware, entry.name());
            }
        }
        return names;
    }
}
