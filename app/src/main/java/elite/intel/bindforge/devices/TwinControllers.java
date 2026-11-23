package elite.intel.bindforge.devices;

import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.devices.model.Device;
import elite.intel.devices.model.DeviceIdentity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Attached controllers that share one VID/PID - two sticks of the same model, say a left/right pair that both left the
 * factory as the right-hand one.
 * <p>
 * The game names a controller in {@code DeviceMappings.xml} and {@code .binds} by its VID/PID and nothing else, so it
 * cannot tell such twins apart, and neither can BindForge: My Devices shows them as one row. No name fixes that - one
 * of them needs a different product id, set in its maker's tool. This only finds them, so the screen can say so rather
 * than leave the second stick silently missing (Krondor, 2026-11-22: a second VKB Gladiator read as the first).
 */
public final class TwinControllers {

    private TwinControllers() {
    }

    /**
     * One VID/PID that more than one attached controller reports.
     *
     * @param hardware     the shared VID/PID
     * @param hardwareName what the first of them calls itself
     * @param count        how many are attached, two or more
     */
    public record Twin(HardwareId hardware, String hardwareName, int count) {
    }

    /**
     * @param attached what the Device Service reports right now
     * @return each VID/PID reported by two or more of them, in the order first seen; empty when there is none
     */
    public static List<Twin> of(List<Device> attached) {
        Map<HardwareId, List<Device>> byHardware = new LinkedHashMap<>();
        for (Device device : attached) {
            // WHY: a controller whose GUID could not be read has no VID/PID to share - My Devices leaves it out too.
            Optional<DeviceIdentity> identity = DeviceIdentities.of(device);
            if (identity.isEmpty()) continue;
            HardwareId hardware = new HardwareId(identity.get().vid(), identity.get().pid());
            byHardware.computeIfAbsent(hardware, key -> new ArrayList<>()).add(device);
        }
        List<Twin> twins = new ArrayList<>();
        byHardware.forEach((hardware, devices) -> {
            if (devices.size() > 1) twins.add(new Twin(hardware, devices.getFirst().name(), devices.size()));
        });
        return List.copyOf(twins);
    }
}
