package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceDivergence.Finding;

import java.util.List;

/**
 * Whether the Alias Designer's APPLY would do anything - the button is offered only then (Alan, 2026-10-15). Pure.
 * <p>
 * APPLY promotes the draft and then pushes the master to every installation, with the pending removals and the
 * renames still on their way. So it has work when <strong>the draft changes something</strong>, when <strong>a
 * removal or rename is still waiting</strong>, or when <strong>an installation differs from the master in a way the
 * push repairs</strong> - exactly the rows {@link DivergenceWaysOut} offers Revert for. That last one is the retry
 * after a partial Apply: the installation it failed on still differs.
 * <p>
 * An orphan {@code .buttonMap}, an unreadable file and an entry the master lacks are not APPLY's to fix - the push
 * removes nothing and cannot read the second - so they leave it dimmed rather than lit forever.
 */
public final class ApplyWork {

    private ApplyWork() {
    }

    /**
     * @param draftChanges    what the draft changes, by device ({@link DraftChanges})
     * @param pendingRemovals names the master still has to remove from some installation
     * @param renameWaiting   whether a device still carries a name an installation has to lose
     * @param findings        each installation against the master
     * @param report          the check the findings came from; {@code null} before activation, when there is none
     */
    public static boolean pending(List<String> draftChanges, List<String> pendingRemovals, boolean renameWaiting,
                                  List<Finding> findings, DeviceFilesCheck.Report report) {
        if (!draftChanges.isEmpty() || !pendingRemovals.isEmpty() || renameWaiting) return true;
        if (report == null) return false;
        return findings.stream().anyMatch(finding -> DivergenceWaysOut.of(finding, report).stream()
                .anyMatch(way -> way.kind() == DivergenceWaysOut.Kind.REVERT));
    }
}
