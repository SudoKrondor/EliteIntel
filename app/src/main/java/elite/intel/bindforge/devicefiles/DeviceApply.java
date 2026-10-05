package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceFilesPush.Report;
import elite.intel.db.managers.BindForgeDeviceDraftManager;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * APPLY TO GAME INSTALLS, for the device files: the draft becomes the master, then the master is pushed out.
 * <p>
 * <strong>The draft is promoted before anything is written</strong> (Alan, 2026-10-04). The master records what
 * the user intends, so a push that cannot reach one installation still leaves the edits safe in the master -
 * that installation stops matching it, the install strip shows which, and the next Apply tries again. Nothing
 * the user saved is lost to a failed write.
 * <p>
 * With no draft, Apply still pushes the master. That is the retry after a partial Apply.
 */
public final class DeviceApply {

    /**
     * @param removed devices the draft removed from the master. Their entries are still in every installation,
     *                since the push adds and updates only - they are stored as pending removals, and this is
     *                the same list for the caller's report
     * @param report  what happened in each installation
     */
    public record Result(List<String> removed, Report report) {
        public Result {
            removed = List.copyOf(removed);
            Objects.requireNonNull(report, "report");
        }
    }

    private final Supplier<List<String>> promote;
    private final Supplier<Report> push;

    /**
     * Promotes the stored draft and pushes to the stored installations.
     *
     * @throws IOException if Elite-Intel's data folder cannot be created for Edit History
     */
    public DeviceApply() throws IOException {
        this(BindForgeDeviceDraftManager.getInstance()::promote, new DeviceFilesPush()::push);
    }

    DeviceApply(Supplier<List<String>> promote, Supplier<Report> push) {
        this.promote = promote;
        this.push = push;
    }

    public Result apply() {
        List<String> removed = promote.get();
        return new Result(removed, push.get());
    }
}
