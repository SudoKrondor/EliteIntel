package elite.intel.bindforge.devicefiles;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Everything that disagrees about the device files, ranked by what it costs the user.
 * <p>
 * <strong>One list, not three.</strong> Every kind of disagreement appears together, because splitting them
 * would mean BindForge deciding which kinds deserve attention. What ranks them instead is the severity.
 */
public final class DeviceDivergence {

    private DeviceDivergence() {
    }

    /**
     * How much a finding costs the user.
     * <p>
     * Judged against the bindings, not against the device files alone: that is what separates red from
     * yellow. A missing entry only matters when a binding names that device.
     */
    public enum Severity {
        /** Consistent - every installation says the same thing. Never produced as a finding. */
        GREEN,
        /** Wrong, but nothing stops working. */
        YELLOW,
        /** Will stop bindings working. */
        RED
    }

    /** What the finding is about. The wording belongs to the UI; this is the fact. */
    public enum Issue {
        /** An entry some installations have and others do not. */
        ENTRY_MISSING_FROM_SOME,
        /** One name pointing at different hardware depending on the installation. */
        ENTRY_HARDWARE_DIFFERS,
        /** A {@code .buttonMap} with no entry behind it, so nothing resolves through it. */
        ORPHANED_BUTTON_MAP
    }

    /**
     * @param deviceName  the entry, or the orphan file's name
     * @param installs    the installations this concerns - those lacking the entry, those that disagree, or
     *                    the one holding the orphan
     * @param referenced  whether a binding names this device, which is what decided the severity
     */
    public record Finding(Issue issue, Severity severity, String deviceName,
                          Set<String> installs, boolean referenced) {
    }

    /**
     * Ranks every disagreement, worst first.
     *
     * @param differences      what the installations' entries disagree about
     * @param orphansByInstall each installation's {@code .buttonMap} files that match no entry
     * @param referencedNames  the entry names the bindings actually use
     */
    public static List<Finding> rank(List<DeviceMappingsComparison.Difference> differences,
                                     Map<String, List<Path>> orphansByInstall,
                                     Set<String> referencedNames) {

        List<Finding> findings = new ArrayList<>();

        for (DeviceMappingsComparison.Difference difference : differences) {
            boolean referenced = referencedNames.contains(difference.deviceName());
            findings.add(new Finding(
                    issueOf(difference),
                    // WHY: referenced decides it, per the governing rule that severity is judged against the
                    // bindings. A name nothing binds to can differ between installations all it likes - the
                    // game never looks it up, so nothing breaks.
                    referenced ? Severity.RED : Severity.YELLOW,
                    difference.deviceName(),
                    difference.kind() == DeviceMappingsComparison.Kind.MISSING_FROM_SOME
                            ? difference.absentFrom()
                            : difference.presentIn(),
                    referenced));
        }

        orphansByInstall.forEach((install, orphans) -> orphans.forEach(orphan ->
                findings.add(new Finding(
                        Issue.ORPHANED_BUTTON_MAP,
                        // WHY: always yellow. Nothing resolves through an orphan, so no binding can be
                        // broken by it - the labels inside it are simply never shown.
                        Severity.YELLOW,
                        stemOf(orphan),
                        Set.of(install),
                        false))));

        findings.sort(Comparator
                .comparing((Finding finding) -> finding.severity() == Severity.RED ? 0 : 1)
                .thenComparing(Finding::deviceName));
        return List.copyOf(findings);
    }

    private static Issue issueOf(DeviceMappingsComparison.Difference difference) {
        return difference.kind() == DeviceMappingsComparison.Kind.MISSING_FROM_SOME
                ? Issue.ENTRY_MISSING_FROM_SOME
                : Issue.ENTRY_HARDWARE_DIFFERS;
    }

    private static String stemOf(Path buttonMap) {
        String name = buttonMap.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }
}
