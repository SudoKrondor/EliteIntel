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
 * <strong>One page per controller {@link DeviceNames#needingName} returns.</strong> A controller named in the edits
 * already has its name on My Devices, so it is not asked again.
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
     * @param replaces     the old hardware id the user said this controller is - the identity question's yes for a
     *                     controller {@code .binds} knows only by its id (A17) - or {@code null}. Its bindings are among
     *                     {@code hardwareIds} and {@code bindings}, and move with the name in the same edit
     */
    public record Page(MyDevice device, String defaultName, Set<String> hardwareIds, int bindings,
                       Map<String, String> labels, HardwareId replaces) {
        public Page {
            Objects.requireNonNull(device, "device");
            Objects.requireNonNull(defaultName, "defaultName");
            hardwareIds = Set.copyOf(hardwareIds);
            labels = Map.copyOf(labels);
        }
    }

    /**
     * The pages, in the order My Devices lists the controllers.
     * <p>
     * A hardware-id reference two controllers could both claim - identical hardware, or one's VID+PID reading as the
     * other's PID+VID - goes to the first page only, so no two pages promise the same bindings; for identical
     * hardware either name drives the same controller.
     *
     * @param devices      My Devices, as the screen built it
     * @param taken        every name in use: the edits', the working copy's, Frontier's
     * @param hardwareIds  {@code .binds}' hardware-id references, as {@link BindsDeviceReferences#countsIn}
     *                     reads them
     * @param labels       each controller's generated labels
     */
    public static List<Page> pages(List<MyDevice> devices, Collection<String> taken,
                                   Map<String, Integer> hardwareIds,
                                   Function<MyDevice, Map<String, String>> labels) {
        return pages(devices, taken, hardwareIds, labels, Map.of());
    }

    /**
     * The pages, where some controllers are the identity question's answer for a controller {@code .binds} knows only
     * by its hardware id (Alan, 2026-10-17, A17): such a page promises the bindings on the <strong>old</strong> id too,
     * and says which id it replaces; FINISH moves them with the name.
     *
     * @param replacing the old id each picked controller is, by the picked controller's hardware
     */
    public static List<Page> pages(List<MyDevice> devices, Collection<String> taken,
                                   Map<String, Integer> hardwareIds,
                                   Function<MyDevice, Map<String, String>> labels,
                                   Map<HardwareId, HardwareId> replacing) {
        List<String> used = new ArrayList<>(taken);
        Set<String> claimed = new HashSet<>();
        List<Page> pages = new ArrayList<>();
        for (MyDevice device : DeviceNames.needingName(devices)) {
            String name = DeviceNames.defaultName(device.hardwareName(), used);
            used.add(name);
            Map<String, Integer> found =
                    new LinkedHashMap<>(BindsDeviceReferences.hardwareIdsFor(device.vid(), device.pid(), hardwareIds));
            HardwareId replaces = replacing.get(device.hardware());
            if (replaces != null) {
                found.putAll(BindsDeviceReferences.hardwareIdsFor(replaces.vid(), replaces.pid(), hardwareIds));
            }
            found.keySet().removeIf(id -> !claimed.add(id.toUpperCase(Locale.ROOT)));
            int bindings = found.values().stream().mapToInt(Integer::intValue).sum();
            pages.add(new Page(device, name, new LinkedHashSet<>(found.keySet()), bindings, labels.apply(device),
                    replaces));
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
