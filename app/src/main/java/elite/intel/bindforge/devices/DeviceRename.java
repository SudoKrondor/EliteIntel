package elite.intel.bindforge.devices;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Whether the Device Editor may rename a device, and which names a new name must not clash with (Alan, 2026-10-14,
 * A11). Pure: the reader gathers the names, this decides.
 * <p>
 * <strong>A built-in is renamed by shadowing it</strong> (Alan, 2026-10-16, A15): the user's element goes ahead of
 * Frontier's, which stays for any other controller it covers. Only while the controller is attached - its VID/PID comes
 * from the hardware - and Frontier's name stays usable for it, so the user can go back to it. A device with no name gets
 * one from onboarding.
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
        /** Still under Frontier's name, and not attached - its VID/PID would come from the hardware. */
        BUILT_IN,
        /** Cleared or reset, waiting for APPLY. */
        CLEARED,
        /** Its last rename has not reached every installation and {@code .binds} yet. */
        PENDING
    }

    /** A device's name as the master or the draft holds it, and the old name it still carries, if any. */
    public record Named(String name, String previousName) {
    }

    /** Whose name the editor opens with. */
    public enum Owner {
        /** One of the user's - any name that is not Frontier's. */
        USER,
        /** One of Frontier's element names. */
        FRONTIER
    }

    /** Whether the controller is plugged in now. */
    public enum Presence {
        ATTACHED,
        UNPLUGGED
    }

    /**
     * Enums rather than booleans for whose name it is and whether it is plugged in (review, 2026-10-17): three
     * booleans in a row would compile with any two swapped, and silently change who may rename.
     *
     * @param name                the name the editor opens with, or {@code null}
     * @param owner               whether that name is one of Frontier's
     * @param presence            whether the controller is plugged in now
     * @param cleared             whether the device is cleared or reset in the draft
     * @param masterName          the master's name for this device, or {@code null} when the master has none
     * @param masterPreviousName  the old name the master still carries for this device, or {@code null}
     */
    public static Allowed allowed(String name, Owner owner, Presence presence, boolean cleared,
                                  String masterName, String masterPreviousName) {
        if (name == null) return Allowed.NO_NAME;
        if (owner == Owner.FRONTIER && presence == Presence.UNPLUGGED) return Allowed.BUILT_IN;
        if (cleared) return Allowed.CLEARED;
        if (masterPreviousName != null && !masterPreviousName.equals(masterName)) return Allowed.PENDING;
        return Allowed.YES;
    }

    /**
     * Every name a new name must not clash with: Frontier's, every installation's entries, and every name and old
     * name in the master and the draft - less the device's own, matched exactly. Its own names stay usable: changing
     * only case is a rename, and so is going back to the name the installations still hold.
     * <p>
     * <strong>Frontier's names stay taken, even a device's own</strong> (review, 2026-10-17, A15). A built-in opens
     * under Frontier's name, and a shadow's old name is Frontier's: dropping either would let {@code t-rudder} through
     * beside {@code T-Rudder}, two elements the game cannot tell apart. Going back to Frontier's name is
     * {@link #backToFrontier}'s, spelled exactly.
     *
     * @param own the device's names - its draft name, master name and old name; {@code null}s ignored, and
     *            Frontier's names among them stay taken
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
            if (name != null && !frontier.contains(name)) taken.remove(name);
        });
        return taken;
    }

    /**
     * Whether a typed name goes back to the Frontier element covering the controller - spelled exactly as Frontier
     * spells it (Alan, 2026-10-16, A15). That name is accepted although it is taken; any other spelling of it is not.
     * <p>
     * <strong>Why exactly.</strong> Frontier's element is still in every installation, and the game finds an element
     * ignoring case: a shadow called {@code t-rudder} ahead of {@code T-Rudder}, or going back to {@code TRudder}, would
     * leave two elements the game cannot tell apart, or a user's element that is Frontier's in all but spelling.
     *
     * @param frontierName the Frontier element covering the controller, or {@code null} when none does
     */
    public static boolean backToFrontier(String typed, String frontierName) {
        return frontierName != null && frontierName.equals(typed);
    }
}
