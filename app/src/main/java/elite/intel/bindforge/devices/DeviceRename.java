package elite.intel.bindforge.devices;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Whether the Device Editor may rename a device, and which names a new name must not clash with (Alan, 2026-10-14,
 * A11). Pure: the reader gathers the names, this decides.
 * <p>
 * <strong>Only the user's own named devices are renamed.</strong> A built-in under Frontier's name is A15, and a
 * device with no name gets one from onboarding.
 * <p>
 * <strong>No chains.</strong> A name still waiting as some device's old name is taken, so it cannot be a new name -
 * and a device whose own rename is still waiting on an installation or on {@code .binds} cannot be renamed again until
 * it finishes, since the master keeps one old name per device and a second rename would lose the first.
 */
public final class DeviceRename {

    private DeviceRename() {
    }

    /** Whether the name can be changed, and why not when it cannot. */
    public enum Allowed {
        YES,
        /** No name yet - onboarding names it. */
        NO_NAME,
        /** Still under Frontier's name - A15. */
        BUILT_IN,
        /** Cleared or reset, waiting for APPLY. */
        CLEARED,
        /** Its last rename has not reached every installation and {@code .binds} yet. */
        PENDING
    }

    /** A device's name as the master or the draft holds it, and the old name it still carries, if any. */
    public record Named(String name, String previousName) {
    }

    /**
     * @param name                the name the editor opens with, or {@code null}
     * @param frontierName        whether that name is one of Frontier's
     * @param cleared             whether the device is cleared or reset in the draft
     * @param masterName          the master's name for this device, or {@code null} when the master has none
     * @param masterPreviousName  the old name the master still carries for this device, or {@code null}
     */
    public static Allowed allowed(String name, boolean frontierName, boolean cleared, String masterName,
                                  String masterPreviousName) {
        if (name == null) return Allowed.NO_NAME;
        if (frontierName) return Allowed.BUILT_IN;
        if (cleared) return Allowed.CLEARED;
        if (masterPreviousName != null && !masterPreviousName.equals(masterName)) return Allowed.PENDING;
        return Allowed.YES;
    }

    /**
     * Every name a new name must not clash with: Frontier's, every installation's entries, and every name and old
     * name in the master and the draft - less the device's own, matched exactly. Its own names stay usable: changing
     * only case is a rename, and so is going back to the name the installations still hold.
     *
     * @param own the device's names - its draft name, master name and old name; {@code null}s ignored
     */
    public static Set<String> taken(Collection<String> frontier, Collection<String> entries, List<Named> master,
                                    List<Named> draft, Collection<String> own) {
        Set<String> taken = new LinkedHashSet<>(frontier);
        taken.addAll(entries);
        for (List<Named> rows : List.of(master, draft)) {
            for (Named row : rows) {
                taken.add(row.name());
                if (row.previousName() != null) taken.add(row.previousName());
            }
        }
        own.forEach(name -> {
            if (name != null) taken.remove(name);
        });
        return taken;
    }
}
