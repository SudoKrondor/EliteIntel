package elite.intel.bindforge.devices;

import elite.intel.bindforge.devices.DeviceEntry.HardwareId;
import elite.intel.db.dao.BindForgeDeviceMasterDao.DeviceRow;
import elite.intel.devices.model.Device;
import elite.intel.devices.model.DeviceIdentity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Builds the My Devices list: <strong>the union of live hardware and file entries</strong>, correlated by
 * VID/PID.
 * <p>
 * The two halves answer different questions and neither is sufficient. {@code DeviceMappings.xml} supplies
 * <em>names</em> and never <em>existence</em> - the game writes nothing into it, so a controller only appears
 * there once somebody has named it. Build the list from the file alone and a brand-new stick is invisible
 * until the user edits a file by hand. Build it from the hardware alone and an entry for a controller sold
 * last year vanishes, taking with it the only sign that bindings still reference it.
 * <p>
 * So: every controller attached right now, named or not, plus every entry any installation holds, plugged in
 * or not.
 * <p>
 * A pure function over its inputs, with no service or database access of its own. The rule it encodes is the
 * one the whole screen rests on, and it is worth being able to test without a UI or a game installation.
 */
public final class MyDeviceList {

    private MyDeviceList() {
    }

    /**
     * @param attached    what the Device Service reports right now, in its own order
     * @param fileEntries every entry read from the installations, Frontier's own included - they are filtered
     *                    here rather than by the caller, so the rule lives in one place
     * @param master      the user's device records, which supply the alias column
     * @param stock       Frontier's shipped list, which decides what is a built-in
     * @return attached controllers first, in the order the Device Service gave them, then entries for
     *         hardware that is not present
     */
    public static List<MyDevice> build(List<Device> attached,
                                       Collection<DeviceEntry> fileEntries,
                                       Collection<DeviceRow> master,
                                       FrontierStockDevices stock) {
        Map<HardwareId, DeviceRow> masterByHardware = indexMaster(master);
        Map<HardwareId, String> entryNames = entryNamesByHardware(fileEntries, stock);

        Map<HardwareId, MyDevice> rows = new LinkedHashMap<>();

        for (Device device : attached) {
            Optional<DeviceIdentity> identity = DeviceIdentities.of(device);
            // WHY: a controller whose GUID could not be read is left out rather than added with blank ids.
            // Every column but its name is derived from the VID/PID, and a row keyed on nothing would merge
            // with the next such controller.
            if (identity.isEmpty()) continue;

            HardwareId hardware = new HardwareId(identity.get().vid(), identity.get().pid());
            rows.put(hardware, row(device.name(), entryNames.get(hardware), hardware, true,
                    masterByHardware.get(hardware), stock));
        }

        for (Map.Entry<HardwareId, String> entry : entryNames.entrySet()) {
            HardwareId hardware = entry.getKey();
            // WHY: an entry for hardware already listed above adds only its name, which that row took when it
            // was built. Putting it again would move the row to the end of the list.
            if (rows.containsKey(hardware)) continue;
            rows.put(hardware, row(null, entry.getValue(), hardware, false,
                    masterByHardware.get(hardware), stock));
        }

        return List.copyOf(rows.values());
    }

    private static MyDevice row(String windowsName, String entryName, HardwareId hardware, boolean attached,
                                DeviceRow masterRow, FrontierStockDevices stock) {
        return new MyDevice(
                windowsName,
                entryName,
                hardware.vid(),
                hardware.pid(),
                attached,
                masterRow == null ? null : masterRow.deviceName(),
                stock.covering(hardware.vid(), hardware.pid()).isPresent());
    }

    /**
     * The master's records by hardware, so the alias column is a lookup rather than a query per row.
     * <p>
     * First wins on a collision. Two master records claiming one VID/PID should not exist - the alias is
     * unique and a device is one piece of hardware - and silently preferring the later one would make which
     * alias a row shows depend on insertion order.
     */
    private static Map<HardwareId, DeviceRow> indexMaster(Collection<DeviceRow> master) {
        Map<HardwareId, DeviceRow> byHardware = new LinkedHashMap<>();
        for (DeviceRow row : master) {
            byHardware.putIfAbsent(new HardwareId(row.vid(), row.pid()), row);
        }
        return byHardware;
    }

    /**
     * The name each piece of hardware is known by in the installations, Frontier's own entries excluded.
     * <p>
     * Frontier's are dropped because they are not devices the user added - the game ships 51 of them, most for
     * hardware nobody owns, and listing them all under My Devices would bury the handful that are real. A
     * built-in that is actually <em>attached</em> still appears, because it comes in through the hardware half
     * of the union, marked {@code builtIn}.
     * <p>
     * Several installations normally name the same device identically, so a repeat is expected and the first
     * is kept. Where they genuinely disagree, that is drift, and reporting it is the divergence list's job
     * rather than this one's - a single cell here could only ever show one of the two names.
     */
    private static Map<HardwareId, String> entryNamesByHardware(Collection<DeviceEntry> fileEntries,
                                                               FrontierStockDevices stock) {
        Map<HardwareId, String> names = new LinkedHashMap<>();
        for (DeviceEntry entry : fileEntries) {
            if (stock.ships(entry.name())) continue;
            for (HardwareId hardware : entry.hardware()) {
                names.putIfAbsent(hardware, entry.name());
            }
        }
        return names;
    }

    /** Every entry across the installations given, for feeding {@link #build}. */
    public static List<DeviceEntry> entriesAcross(Collection<List<DeviceEntry>> perInstallation) {
        List<DeviceEntry> all = new ArrayList<>();
        perInstallation.forEach(all::addAll);
        return all;
    }
}
