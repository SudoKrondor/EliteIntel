package elite.intel.bindforge.devices;

import elite.intel.bindforge.devicefiles.DeviceDivergence.Finding;
import elite.intel.bindforge.devicefiles.DeviceDivergence.Issue;
import elite.intel.bindforge.devicefiles.DeviceDivergence.Severity;
import elite.intel.bindforge.devicefiles.DeviceEntry;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Installations column of My Devices: for one device, whether each installation has caught up with the master.
 * <p>
 * <strong>A readout, never a switch.</strong> Each marker reports a fact about what is on disk; there is no
 * per-installation editor behind it (alias-designer.md, <em>My Devices</em>).
 * <p>
 * <strong>The colour is the divergence list's</strong>, read from its findings rather than judged again here, so a
 * marker and the row explaining it can never disagree. The text says what the installation holds and the colour
 * says what that costs: an entry the master holds and an installation lacks reads <em>not added</em>, in red when a
 * binding names it (Alan, 2026-10-08).
 * <p>
 * Only once the master exists - before first setup there is nothing to match, and the column is not shown. Pure,
 * so the rule can be tested without a screen or a game installation.
 */
public final class InstallationMarkers {

    private InstallationMarkers() {
    }

    public enum State {
        /** The installation holds what the master says for this device. */
        MATCHES,
        /** The installation holds no entry for this device. */
        NOT_ADDED,
        /** The installation differs from the master for this device. */
        DIFFERS,
        /** The installation's folder is not there, so nothing could be compared. */
        NOT_FOUND
    }

    /**
     * @param severity the colour: {@link Severity#GREEN} for a match, the worst finding's for a difference, and
     *                 {@code null} where there is nothing to judge - a device neither the master nor the installation
     *                 holds, or an installation that is not there
     */
    public record Marker(State state, Severity severity) {
    }

    /**
     * @param device       the My Devices row
     * @param installs     every installation, keyed as {@code DeviceDivergence.installKey} keys them, in the order the
     *                     column shows them
     * @param notFound     those whose folder is gone
     * @param entries      what each installation whose {@code DeviceMappings.xml} could be read holds. An installation
     *                     absent here and not in {@code notFound} could not be read, which its finding says
     * @param findings     the divergence list against the master
     * @return one marker per installation, in {@code installs} order
     */
    public static Map<String, Marker> of(MyDevice device, List<String> installs, Set<String> notFound,
                                         Map<String, List<DeviceEntry>> entries, Collection<Finding> findings) {
        Map<String, Marker> markers = new LinkedHashMap<>();
        for (String install : installs) {
            markers.put(install, notFound.contains(install)
                    ? new Marker(State.NOT_FOUND, null)
                    : markerFor(device, install, entries.getOrDefault(install, List.of()), findings));
        }
        return markers;
    }

    private static Marker markerFor(MyDevice device, String install, List<DeviceEntry> entries,
                                    Collection<Finding> findings) {
        Set<String> names = namesFor(device, entries);
        boolean holdsEntry = entries.stream().anyMatch(entry -> entry.hardware().contains(device.hardware()));

        Severity worst = null;
        boolean entryMissing = false;
        for (Finding finding : findings) {
            if (!finding.installs().contains(install) || !concerns(finding, device, names)) continue;
            // WHY: compareTo ranks Severity mildest to worst, as its declaration says.
            if (worst == null || finding.severity().compareTo(worst) > 0) worst = finding.severity();
            if (finding.issue() == Issue.ENTRY_MISSING) entryMissing = true;
        }

        if (worst != null) return new Marker(entryMissing ? State.NOT_ADDED : State.DIFFERS, worst);
        // WHY: a match needs something to match. A built-in needs Frontier's entry there, whether or not the master
        // labels it - the check compares only the user's elements, so a Frontier entry gone from an installation
        // raises no finding (review, 2026-10-08). One of the user's devices the master holds needs nothing more: an
        // installation lacking it has an ENTRY_MISSING finding and never gets here. Any other entry, such as a
        // pending removal, is not the master's, so the installation has not "added" the device either - which is
        // what the Alias column says.
        boolean matches = device.builtIn() ? holdsEntry : device.alias() != null;
        if (matches) return new Marker(State.MATCHES, Severity.GREEN);
        return new Marker(State.NOT_ADDED, null);
    }

    /**
     * Whether a finding is about this device in its installation.
     * <ul>
     *     <li>By name: the master's name, or any name the installation holds for this hardware - an entry the master
     *     lacks is named only there.</li>
     *     <li>An unreadable {@code DeviceMappings.xml} concerns every device: nothing is known about any of them.</li>
     *     <li>A reset one concerns every device the master holds - the one row says the file, not which entries -
     *     and its colour is that row's (Alan, 2026-10-08). A device the master does not hold is judged by what the
     *     reset file holds, which the entries already say.</li>
     * </ul>
     */
    private static boolean concerns(Finding finding, MyDevice device, Set<String> names) {
        return switch (finding.issue()) {
            case FILE_UNREADABLE -> true;
            case FILE_RESET -> device.alias() != null;
            default -> names.contains(finding.deviceName());
        };
    }

    /** The master's name for the device, and every name the installation gives its hardware. */
    private static Set<String> namesFor(MyDevice device, List<DeviceEntry> entries) {
        Set<String> names = new HashSet<>();
        if (device.alias() != null) names.add(device.alias());
        for (DeviceEntry entry : entries) {
            if (entry.hardware().contains(device.hardware())) names.add(entry.name());
        }
        return names;
    }
}
