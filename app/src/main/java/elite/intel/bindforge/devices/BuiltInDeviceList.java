package elite.intel.bindforge.devices;

import elite.intel.bindforge.devicefiles.DeviceEntry;
import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devicefiles.FrontierStockDevices;
import elite.intel.devices.model.Device;
import elite.intel.devices.model.DeviceIdentity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The Built-in Devices list: Frontier's shipped entries, read-only (Alan, 2026-10-08).
 * <p>
 * Built from the reference in the jar rather than from any installation's file, because an installation's file
 * mixes Frontier's entries with the user's and nothing in it says which is which. The list is the reference, in
 * the order Frontier's file gives it.
 */
public final class BuiltInDeviceList {

    private BuiltInDeviceList() {
    }

    /**
     * @param stock    Frontier's shipped list
     * @param attached what the Device Service reports right now - empty when it is not running, which leaves every
     *                 row unattached rather than failing
     * @return one row per Frontier element, in file order
     */
    public static List<BuiltInDevice> build(FrontierStockDevices stock, List<Device> attached) {
        Set<String> attachedNames = attachedEntryNames(stock, attached);
        List<BuiltInDevice> rows = new ArrayList<>();
        for (DeviceEntry entry : stock.entries()) {
            rows.add(rowOf(entry, attachedNames.contains(entry.name())));
        }
        return List.copyOf(rows);
    }

    /**
     * One entry as a row: its own pair as the primary, every other pair as an alternative. An element with no pair
     * of its own has no primary, and all its pairs are alternatives - none of them is promoted to stand in for it.
     * <p>
     * Package-private as a test seam: no shipped element has that shape, and Frontier's list cannot be built by hand
     * outside its own package.
     */
    static BuiltInDevice rowOf(DeviceEntry entry, boolean attached) {
        List<HardwareId> alternatives = entry.hardware().stream()
                .filter(pair -> !pair.equals(entry.primary()))
                .toList();
        return new BuiltInDevice(entry.name(), entry.primary(), alternatives, attached);
    }

    /**
     * The entries an attached controller resolves to.
     * <p>
     * Through {@link FrontierStockDevices#covering}, which takes the first entry claiming a pair, as the game
     * does - so where two of Frontier's entries claim one controller only the one the game uses reads attached,
     * and the list agrees with My Devices marking the same controller BUILT-IN.
     */
    private static Set<String> attachedEntryNames(FrontierStockDevices stock, List<Device> attached) {
        Set<String> names = new HashSet<>();
        for (Device device : attached) {
            Optional<DeviceIdentity> identity = DeviceIdentities.of(device);
            if (identity.isEmpty()) continue;
            stock.covering(identity.get().vid(), identity.get().pid())
                    .ifPresent(entry -> names.add(entry.name()));
        }
        return names;
    }
}
