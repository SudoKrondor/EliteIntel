package elite.intel.bindforge.devices;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Compares the device entries of several installations.
 * <p>
 * Every installation is supposed to hold the same device files. Anywhere they disagree is something to
 * resolve, because a shared {@code .binds} names devices by entries that live in these per-installation
 * files - so an installation missing one cannot resolve the binding.
 * <p>
 * <strong>This reports the differences, not their severity.</strong> Ranking them needs the shared
 * {@code .binds}: a missing entry only costs the user something when a binding actually names that device,
 * which is what separates "will break" from "untidy".
 */
public final class DeviceMappingsComparison {

    private DeviceMappingsComparison() {
    }

    /** What is different about one device entry across the installations that were compared. */
    public enum Kind {
        /** Present in some installations and absent from others. */
        MISSING_FROM_SOME,
        /** Present everywhere, but the hardware ids behind the name do not agree. */
        HARDWARE_DIFFERS
    }

    /**
     * @param deviceName      the element tag, which is what a {@code .binds} file names
     * @param kind            what is different
     * @param presentIn       the installations holding an entry under this name
     * @param absentFrom      the installations with no entry under this name
     * @param hardwareByInstall what each installation says the hardware is, for a disagreement
     */
    public record Difference(String deviceName,
                             Kind kind,
                             Set<String> presentIn,
                             Set<String> absentFrom,
                             Map<String, Set<DeviceEntry.HardwareId>> hardwareByInstall) {
    }

    /**
     * Compares each installation's entries against the others.
     *
     * @param entriesByInstall the parsed entries, keyed by however the caller names an installation
     * @return one difference per device name that does not agree, in device-name order; empty when every
     *         installation holds the same entries, which is the state BindForge is trying to reach
     */
    public static List<Difference> compare(Map<String, List<DeviceEntry>> entriesByInstall) {
        if (entriesByInstall.size() < 2) return List.of();

        Map<String, Map<String, Set<DeviceEntry.HardwareId>>> byDevice = indexByDeviceName(entriesByInstall);
        List<Difference> differences = new ArrayList<>();

        for (Map.Entry<String, Map<String, Set<DeviceEntry.HardwareId>>> device : byDevice.entrySet()) {
            String deviceName = device.getKey();
            Map<String, Set<DeviceEntry.HardwareId>> hardwareByInstall = device.getValue();

            Set<String> presentIn = new LinkedHashSet<>(hardwareByInstall.keySet());
            Set<String> absentFrom = new LinkedHashSet<>(entriesByInstall.keySet());
            absentFrom.removeAll(presentIn);

            if (!absentFrom.isEmpty()) {
                differences.add(new Difference(deviceName, Kind.MISSING_FROM_SOME,
                        Set.copyOf(presentIn), Set.copyOf(absentFrom), Map.copyOf(hardwareByInstall)));
            } else if (hardwareDisagrees(hardwareByInstall)) {
                differences.add(new Difference(deviceName, Kind.HARDWARE_DIFFERS,
                        Set.copyOf(presentIn), Set.of(), Map.copyOf(hardwareByInstall)));
            }
        }
        return List.copyOf(differences);
    }

    /**
     * Device name to installation to hardware, keeping device names in sorted order so the result is stable
     * between runs rather than following whichever installation happened to be read first.
     */
    private static Map<String, Map<String, Set<DeviceEntry.HardwareId>>> indexByDeviceName(
            Map<String, List<DeviceEntry>> entriesByInstall) {

        Map<String, Map<String, Set<DeviceEntry.HardwareId>>> byDevice = new LinkedHashMap<>();
        for (String deviceName : allDeviceNames(entriesByInstall)) {
            Map<String, Set<DeviceEntry.HardwareId>> perInstall = new LinkedHashMap<>();
            entriesByInstall.forEach((install, entries) -> entries.stream()
                    .filter(entry -> entry.name().equals(deviceName))
                    .findFirst()
                    .ifPresent(entry -> perInstall.put(install, entry.hardware())));
            byDevice.put(deviceName, perInstall);
        }
        return byDevice;
    }

    private static Set<String> allDeviceNames(Map<String, List<DeviceEntry>> entriesByInstall) {
        Set<String> names = new TreeSet<>();
        entriesByInstall.values().forEach(entries -> entries.forEach(entry -> names.add(entry.name())));
        return names;
    }

    private static boolean hardwareDisagrees(Map<String, Set<DeviceEntry.HardwareId>> hardwareByInstall) {
        Set<DeviceEntry.HardwareId> first = hardwareByInstall.values().iterator().next();
        return hardwareByInstall.values().stream().anyMatch(hardware -> !hardware.equals(first));
    }
}
