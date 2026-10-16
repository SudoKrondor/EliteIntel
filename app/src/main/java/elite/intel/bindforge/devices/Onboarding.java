package elite.intel.bindforge.devices;

import elite.intel.bindforge.devicefiles.BindsDeviceReferences;
import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;

import java.util.*;
import java.util.function.Function;

/**
 * Onboarding's pages: who is asked for a name, what each is offered, and whether a typed name may be used (spec,
 * alias-designer.md, <em>Onboarding</em>; Alan, 2026-10-14, A6b). Pure - the caller reads the hardware, the names
 * already taken and the bindings.
 * <p>
 * <strong>One page per controller {@link DeviceNames#needingName} returns</strong>, less any whose hardware the
 * draft already names: a controller named for Apply is waiting, not unnamed, and asking again would give it a
 * second name.
 * <p>
 * <strong>The defaults never clash with each other.</strong> Two sticks reporting the same name are offered
 * {@code VirpilControls20220720} and {@code VirpilControls202207202}, as the default-name rule numbers any clash.
 * A typed name is held to {@link DeviceNames#validate} against everything taken <em>and the names on the other
 * pages</em>, so the dialog can never write two controllers under one name.
 */
public final class Onboarding {

    private Onboarding() {
    }

    /**
     * One controller to name.
     *
     * @param defaultName  what it is offered, and what Skip takes
     * @param hardwareIds  the values {@code .binds} names it by, exactly as found - empty when no binding names it by
     *                     hardware id, in which case it is named straight away
     * @param bindings     how many bindings name it by hardware id - what moves to its name on Apply
     * @param labels       its generated labels, by input token
     */
    public record Page(MyDevice device, String defaultName, Set<String> hardwareIds, int bindings,
                       Map<String, String> labels) {
        public Page {
            Objects.requireNonNull(device, "device");
            Objects.requireNonNull(defaultName, "defaultName");
            hardwareIds = Set.copyOf(hardwareIds);
            labels = Map.copyOf(labels);
        }

        /** Whether naming it moves bindings, so it waits for Apply. */
        public boolean waitsForApply() {
            return bindings > 0;
        }
    }

    /**
     * The pages, in the order My Devices lists the controllers.
     * <p>
     * A hardware-id reference two controllers could both claim - identical hardware, or one's VID+PID reading as the
     * other's PID+VID - goes to the first page only, so no two pages promise the same bindings. Apply moves them by
     * the master's order (see {@code DeviceApply}); for identical hardware either name drives the same controller.
     *
     * @param devices      My Devices, as the screen built it
     * @param heldByDraft  hardware the draft already names - controllers named for Apply and not yet applied
     * @param taken        every name in use: the master's, the draft's, every installation's entries, Frontier's
     * @param hardwareIds  {@code .binds}' hardware-id references, as {@link BindsDeviceReferences#hardwareIdCounts}
     *                     reads them
     * @param labels       each controller's generated labels
     */
    public static List<Page> pages(List<MyDevice> devices, Collection<HardwareId> heldByDraft,
                                   Collection<String> taken, Map<String, Integer> hardwareIds,
                                   Function<MyDevice, Map<String, String>> labels) {
        Set<HardwareId> held = Set.copyOf(heldByDraft);
        List<String> used = new ArrayList<>(taken);
        Set<String> claimed = new HashSet<>();
        List<Page> pages = new ArrayList<>();
        for (MyDevice device : DeviceNames.needingName(devices)) {
            if (held.contains(device.hardware())) continue;
            String name = DeviceNames.defaultName(device.hardwareName(), used);
            used.add(name);
            Map<String, Integer> found =
                    new LinkedHashMap<>(BindsDeviceReferences.hardwareIdsFor(device.vid(), device.pid(), hardwareIds));
            found.keySet().removeIf(id -> !claimed.add(id.toUpperCase(Locale.ROOT)));
            int bindings = found.values().stream().mapToInt(Integer::intValue).sum();
            pages.add(new Page(device, name, new LinkedHashSet<>(found.keySet()), bindings, labels.apply(device)));
        }
        return pages;
    }

    /**
     * Whether the name typed on one page may be used: the rule, against every name taken and the names chosen on
     * the other pages.
     *
     * @param chosen the name on each page, in page order - {@code page}'s own is left out
     */
    public static DeviceNames.Verdict check(String typed, int page, List<String> chosen, Collection<String> taken) {
        List<String> others = new ArrayList<>(taken);
        for (int i = 0; i < chosen.size(); i++) {
            if (i != page && chosen.get(i) != null) others.add(chosen.get(i));
        }
        return DeviceNames.validate(typed, others);
    }
}
