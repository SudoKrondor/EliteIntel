package elite.intel.bindforge.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Which of the user's named controllers are not attached while bindings name them - the once-per-run warning's
 * list (Alan, 2026-10-11, A13).
 * <p>
 * <strong>Only what {@code .binds} names.</strong> The warning exists because the game will not load bindings for a
 * controller that is not there. A named stick no binding uses costs nothing when it is unplugged, and an entry for
 * hardware sold years ago would otherwise be announced on every run.
 * <p>
 * <strong>The user's names only.</strong> A row whose name is one of Frontier's is left out, by name rather than by
 * {@link MyDevice#builtIn()}: that flag is decided by VID/PID for anything the edits do not name, so it would also
 * hide a user's own element shadowing a built-in from before first setup.
 * <p>
 * A pure function over its inputs, so the rule is testable without hardware, files or a voice.
 */
public final class MissingControllers {

    private MissingControllers() {
    }

    /** One controller to name in the warning, and how many bindings name it. */
    public record Missing(String name, int bindings) {
    }

    /**
     * @param devices         the My Devices rows, attached and not - the union, as the screen lists it
     * @param referenceCounts how many bindings name each entry, as {@code BindsDeviceReferences.referenceCounts}
     *                        gives them
     * @param frontierNames   every element name Frontier ships
     * @return the named controllers not attached that bindings name, in the order the rows give them, each once
     */
    public static List<Missing> of(List<MyDevice> devices,
                                   Map<String, Integer> referenceCounts,
                                   Collection<String> frontierNames) {
        Map<String, Integer> counts = new HashMap<>();
        // WHY: summed, because the game matches Device= ignoring case and one file may spell a name two ways.
        referenceCounts.forEach((name, count) -> counts.merge(key(name), count, Integer::sum));
        Set<String> frontier = new HashSet<>();
        frontierNames.forEach(name -> frontier.add(key(name)));

        // WHY: two rows can share a name before first setup - one per VID/PID an installation gave it. If either is
        // attached the name resolves to hardware that is here, and saying it is missing would be wrong.
        Set<String> attachedNames = new HashSet<>();
        for (MyDevice device : devices) {
            String name = nameOf(device);
            if (device.attached() && name != null) attachedNames.add(key(name));
        }

        Set<String> seen = new LinkedHashSet<>();
        List<Missing> missing = new ArrayList<>();
        for (MyDevice device : devices) {
            String name = nameOf(device);
            if (device.attached() || name == null) continue;
            String key = key(name);
            if (frontier.contains(key) || attachedNames.contains(key) || !seen.add(key)) continue;
            Integer bindings = counts.get(key);
            if (bindings == null || bindings <= 0) continue;
            missing.add(new Missing(name, bindings));
        }
        return List.copyOf(missing);
    }

    /**
     * The edits' name when they give one, else the working copy's. The edits are what the next APPLY writes, so
     * where the working copy still has the old name, the edits' name is the one meant.
     */
    private static String nameOf(MyDevice device) {
        return device.alias() != null ? device.alias() : device.entryName();
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
