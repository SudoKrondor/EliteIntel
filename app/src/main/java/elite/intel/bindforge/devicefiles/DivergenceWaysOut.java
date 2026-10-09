package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceDivergence.Finding;
import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.ElementChange;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.InstallationCheck;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The ways out of one row of the divergence list, once the master exists. Pure: which ones a row gets is decided
 * here, and the screen only shows them. None runs until the user picks it and confirms.
 * <p>
 * Settled 2026-10-08 (Alan) - alias-designer.md, <em>The ways out</em>:
 * <ul>
 *     <li><strong>Revert</strong> pushes the master to each installation the row names. It is the whole
 *     installation, not the row - C3's revert.</li>
 *     <li><strong>Adopt</strong> takes one device as one installation holds it into the master, and nothing else
 *     that installation differs on - offered only where the installation holds something to take: other hardware,
 *     or an entry the master lacks.</li>
 *     <li><strong>Re-merge</strong> is the label merge with the master as one side.</li>
 *     <li>An unreadable {@code DeviceMappings.xml} and an orphan {@code .buttonMap} get none: the push cannot read
 *     the first either, and the second needs a file removed, which is CLEAR's.</li>
 * </ul>
 */
public final class DivergenceWaysOut {

    private DivergenceWaysOut() {
    }

    public enum Kind {
        /** Push the master to the installations - {@link DeviceFilesPush#push(long)}, once each. */
        REVERT,
        /** Take the device as one installation holds it - {@link DeviceFilesAdopt#adopt(long, String)}. */
        ADOPT,
        /** Merge the labels with the master as one side - {@link DeviceFilesAdopt#adoptLabels}. */
        REMERGE
    }

    /** Why a row has no way out, for the screen to say. */
    public enum NoWayOut {
        /** The file cannot be read, so it cannot be rewritten either - restore it, or fix it by hand. */
        FILE_UNREADABLE,
        /** Removing a {@code .buttonMap} comes with CLEAR. */
        ORPHAN
    }

    /**
     * @param installIds for {@link Kind#REVERT} and {@link Kind#REMERGE} every installation the row names; for
     *                   {@link Kind#ADOPT} the one installation adopted from
     * @param hardware   for {@link Kind#ADOPT}, the VID/PID that installation holds; {@code null} otherwise
     */
    public record WayOut(Kind kind, List<Long> installIds, HardwareId hardware) {
        public WayOut {
            Objects.requireNonNull(kind, "kind");
            installIds = List.copyOf(installIds);
        }
    }

    /**
     * @param finding a finding against the master - its installations keyed by {@link DeviceDivergence#installKey}
     * @param report  the check the finding came from, which holds what each installation's element is
     * @return the ways out, revert first; empty when there is none - see {@link #whyNone}
     */
    public static List<WayOut> of(Finding finding, DeviceFilesCheck.Report report) {
        List<Long> installs = finding.installs().stream().map(DeviceDivergence::installIdOf).sorted().toList();
        List<WayOut> ways = new ArrayList<>();
        switch (finding.issue()) {
            case ENTRY_MISSING, ENTRY_OUT_OF_PLACE, BUTTON_MAP_MISSING, BUTTON_MAP_UNREADABLE, FILE_RESET ->
                    ways.add(new WayOut(Kind.REVERT, installs, null));
            case ENTRY_HARDWARE_DIFFERS -> {
                ways.add(new WayOut(Kind.REVERT, installs, null));
                addAdopts(ways, finding.deviceName(), installs, report);
            }
            // WHY: no revert. The push never removes, so reverting leaves an entry the master lacks where it is.
            case ENTRY_NOT_IN_MASTER -> addAdopts(ways, finding.deviceName(), installs, report);
            case LABELS_DIFFER -> {
                ways.add(new WayOut(Kind.REMERGE, installs, null));
                ways.add(new WayOut(Kind.REVERT, installs, null));
            }
            case FILE_UNREADABLE, ORPHANED_BUTTON_MAP -> {
            }
        }
        return List.copyOf(ways);
    }

    /** Why {@link #of} offers nothing for this finding, or {@code null} when it offers something. */
    public static NoWayOut whyNone(Finding finding) {
        return switch (finding.issue()) {
            case FILE_UNREADABLE -> NoWayOut.FILE_UNREADABLE;
            case ORPHANED_BUTTON_MAP -> NoWayOut.ORPHAN;
            default -> null;
        };
    }

    /** One adopt per installation, each carrying the hardware that installation holds for the device. */
    private static void addAdopts(List<WayOut> ways, String deviceName, List<Long> installs,
                                  DeviceFilesCheck.Report report) {
        for (long install : installs) {
            HardwareId hardware = hardwareIn(report, install, deviceName);
            if (hardware != null) ways.add(new WayOut(Kind.ADOPT, List.of(install), hardware));
        }
    }

    private static HardwareId hardwareIn(DeviceFilesCheck.Report report, long installId, String deviceName) {
        for (InstallationCheck installation : report.installations()) {
            if (installation.target().installId() != installId) continue;
            for (ElementChange element : installation.elements()) {
                if (element.deviceName().equals(deviceName)) return element.installation();
            }
        }
        return null;
    }
}
