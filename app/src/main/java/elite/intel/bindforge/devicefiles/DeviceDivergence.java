package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceFilesCheck.ElementChange;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.ElementKind;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.InstallationCheck;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.LabelChange;
import elite.intel.bindforge.install.GameInstallation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Everything that disagrees about the device files, ranked by what it costs the user.
 * <p>
 * <strong>One list, not three.</strong> Every kind of disagreement appears together, because splitting them
 * would mean BindForge deciding which kinds deserve attention. What ranks them instead is the severity.
 * <p>
 * <strong>Against the master once there is one</strong> - {@link #againstMaster}. Before first setup there is
 * nothing to compare against, so the installations are compared with each other - {@link #rank} - which is what
 * shows the user why first setup is worth running (Alan, 2026-10-08).
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
        /** An entry these installations lack - which the master holds, or before first setup, others hold. */
        ENTRY_MISSING,
        /** One name on different hardware - from the master's, or before first setup, between installations. */
        ENTRY_HARDWARE_DIFFERS,
        /** A {@code .buttonMap} with no entry behind it, so nothing resolves through it. */
        ORPHANED_BUTTON_MAP,
        /** The master's entry is there, but below Frontier's for the same hardware, which the game uses instead. */
        ENTRY_OUT_OF_PLACE,
        /** An entry of the user's the installation holds and the master does not, so Apply never spreads it. */
        ENTRY_NOT_IN_MASTER,
        /** The device's labels differ from the master's. */
        LABELS_DIFFER,
        /** The master has labels for the device; the installation has no {@code .buttonMap} for it. */
        BUTTON_MAP_MISSING,
        /** The installation's {@code .buttonMap} for the device is not well-formed. */
        BUTTON_MAP_UNREADABLE,
        /** The installation's {@code DeviceMappings.xml} is Frontier's stock file, or is gone. */
        FILE_RESET,
        /** The installation's {@code DeviceMappings.xml} could not be read. */
        FILE_UNREADABLE
    }

    /**
     * @param deviceName  the entry, the orphan file's name, or {@code DeviceMappings.xml} for a finding about
     *                    the whole file
     * @param installs    the installations this concerns - those lacking the entry, those that disagree, or
     *                    the one holding the orphan
     * @param referenced  whether a binding names this device, which is what decided the severity
     */
    public record Finding(Issue issue, Severity severity, String deviceName,
                          Set<String> installs, boolean referenced) {
    }

    /**
     * Before first setup: ranks every disagreement between the installations, worst first.
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

    /**
     * Once the master exists: ranks every way the installations differ from it, worst first. One row per device
     * and issue, naming every installation it concerns.
     * <ul>
     *     <li>An element missing, on other hardware, out of place or not in the master is <strong>red when a
     *     binding names it</strong>, yellow otherwise. Out of place is red because the game uses Frontier's entry
     *     for that hardware and never resolves the name the bindings use (domain doc §1.2d). Not in the master is
     *     red because Apply will never put it in the installations that lack it.</li>
     *     <li>A reset {@code DeviceMappings.xml} is <strong>one row</strong>, not one per entry it lost and not
     *     one per {@code .buttonMap} beside it - the repair is one revert, which rewrites both. Reset to Frontier's
     *     file, it is red when a binding names one of the user's own devices, the entries it lost; Frontier's still
     *     resolve. With no file at all it is always red: the game then loads no bindings (overview, <em>Why
     *     {@code DeviceMappings.xml} is not cosmetic</em>).</li>
     *     <li>An unreadable {@code DeviceMappings.xml} is one row, red when a binding names any master device.</li>
     *     <li>Labels are always yellow: a wrong label breaks no binding.</li>
     *     <li>A {@code .buttonMap} left behind by an entry the installation lacks is not listed as an orphan as
     *     well: the missing entry is the one problem.</li>
     *     <li>Not listed: an installation that is unchanged or whose folder is gone, and a {@code .buttonMap} whose
     *     labels agree and whose layout does not - nothing there for the user to judge.</li>
     * </ul>
     * Settled 2026-10-08 (Alan) - alias-designer.md, <em>Against the master</em>.
     *
     * @param report           {@link DeviceFilesCheck#check()}, which must have found a master
     * @param orphansByInstall each installation's {@code .buttonMap} files that match no entry, keyed as
     *                         {@link #installKey} keys them
     * @param referencedNames  the entry names the bindings actually use
     * @throws IllegalArgumentException if the report found no master - an empty list would read as "everything
     *                                  matches"
     */
    public static List<Finding> againstMaster(DeviceFilesCheck.Report report, Map<String, List<Path>> orphansByInstall,
                                              Set<String> referencedNames) {
        if (!report.setUp()) {
            throw new IllegalArgumentException("no master to compare against - first setup has not run");
        }
        Map<Row, Set<String>> rows = new LinkedHashMap<>();
        Map<String, Set<String>> missingByInstall = new LinkedHashMap<>();
        boolean masterReferenced = report.masterNames().stream().anyMatch(referencedNames::contains);
        boolean userDeviceReferenced = report.userNames().stream().anyMatch(referencedNames::contains);

        for (InstallationCheck installation : report.installations()) {
            String install = installKey(installation.target().installId());
            switch (installation.state()) {
                case UNCHANGED, MISSING -> {
                }
                case UNREADABLE -> add(rows, new Row(Issue.FILE_UNREADABLE, GameInstallation.DEVICE_MAPPINGS,
                        masterReferenced), install);
                case WIPED -> add(rows, new Row(Issue.FILE_RESET, GameInstallation.DEVICE_MAPPINGS,
                        installation.fileGone() || userDeviceReferenced), install);
                case EDITED -> {
                    Set<String> notInMaster = new HashSet<>();
                    for (ElementChange element : installation.elements()) {
                        if (element.kind() == ElementKind.ADDED) notInMaster.add(element.deviceName());
                        if (element.kind() == ElementKind.MISSING) {
                            missingByInstall.computeIfAbsent(install, key -> new HashSet<>()).add(element.deviceName());
                        }
                        add(rows, new Row(issueOf(element.kind()), element.deviceName(),
                                referencedNames.contains(element.deviceName())), install);
                    }
                    addLabels(rows, installation, notInMaster, referencedNames, install);
                }
            }
        }

        orphansByInstall.forEach((install, orphans) -> orphans.forEach(orphan -> {
            String stem = stemOf(orphan);
            if (missingByInstall.getOrDefault(install, Set.of()).contains(stem)) return;
            add(rows, new Row(Issue.ORPHANED_BUTTON_MAP, stem, false), install);
        }));

        List<Finding> findings = new ArrayList<>();
        rows.forEach((row, installs) -> findings.add(new Finding(row.issue(), severityOf(row), row.deviceName(),
                Set.copyOf(installs), row.referenced())));
        findings.sort(Comparator
                .comparing((Finding finding) -> finding.severity() == Severity.RED ? 0 : 1)
                .thenComparing(Finding::deviceName));
        return List.copyOf(findings);
    }

    /**
     * How a finding names an installation once the master exists: its stored id. The Alias Designer screen keys
     * its installation names with this too, so the two cannot drift apart.
     */
    public static String installKey(long installId) {
        return String.valueOf(installId);
    }

    /** The installation id a finding against the master names - the inverse of {@link #installKey}. */
    public static long installIdOf(String installKey) {
        return Long.parseLong(installKey);
    }

    /**
     * One row per device for its labels, however many inputs differ - the inputs themselves are the re-merge's to
     * show. A device the master does not hold is skipped: its own row already says so, and its labels are often
     * the thirty-odd a generated {@code .buttonMap} carries.
     */
    private static void addLabels(Map<Row, Set<String>> rows, InstallationCheck installation, Set<String> notInMaster,
                                  Set<String> referencedNames, String install) {
        for (LabelChange label : installation.labels()) {
            if (notInMaster.contains(label.deviceName())) continue;
            Issue issue = switch (label.kind()) {
                case ADDED, REMOVED, CHANGED -> Issue.LABELS_DIFFER;
                case FILE_MISSING -> Issue.BUTTON_MAP_MISSING;
                case FILE_UNREADABLE -> Issue.BUTTON_MAP_UNREADABLE;
                case LAYOUT_ONLY -> null;
            };
            if (issue != null) {
                add(rows, new Row(issue, label.deviceName(), referencedNames.contains(label.deviceName())), install);
            }
        }
    }

    private static Issue issueOf(ElementKind kind) {
        return switch (kind) {
            case MISSING -> Issue.ENTRY_MISSING;
            case HARDWARE_DIFFERS -> Issue.ENTRY_HARDWARE_DIFFERS;
            case OUT_OF_PLACE -> Issue.ENTRY_OUT_OF_PLACE;
            case ADDED -> Issue.ENTRY_NOT_IN_MASTER;
        };
    }

    private static Severity severityOf(Row row) {
        return switch (row.issue()) {
            // WHY: always yellow. A label, or the file holding labels, breaks no binding however many name the
            // device - the game shows the raw input instead.
            case LABELS_DIFFER, BUTTON_MAP_MISSING, BUTTON_MAP_UNREADABLE, ORPHANED_BUTTON_MAP -> Severity.YELLOW;
            default -> row.referenced() ? Severity.RED : Severity.YELLOW;
        };
    }

    private static void add(Map<Row, Set<String>> rows, Row row, String install) {
        rows.computeIfAbsent(row, key -> new LinkedHashSet<>()).add(install);
    }

    /**
     * What a finding is grouped by, so one problem in three installations is one row naming three. For a whole-file
     * row, {@code referenced} is whether the bindings lose anything by it.
     */
    private record Row(Issue issue, String deviceName, boolean referenced) {
    }

    private static Issue issueOf(DeviceMappingsComparison.Difference difference) {
        return difference.kind() == DeviceMappingsComparison.Kind.MISSING_FROM_SOME
                ? Issue.ENTRY_MISSING
                : Issue.ENTRY_HARDWARE_DIFFERS;
    }

    private static String stemOf(Path buttonMap) {
        String name = buttonMap.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }
}
