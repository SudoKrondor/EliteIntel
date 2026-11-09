package elite.intel.bindforge.storage;

import java.util.Objects;

/**
 * What one write through {@link WritePath} did - first-time startup's, RESTORE's, the check's and APPLY's.
 *
 * @param report          each place, as the write path reports it
 * @param backup          the backup taken before it
 * @param setUpAskedAgain whether the write should have marked first-time startup complete and could not, so the next
 *                        click on the BindForge tab offers it again - the user is told (reviews, 2026-10-25 and
 *                        2026-10-27). Always false for APPLY, which never touches the flag
 * @param leftovers       what the check moved from the bindings folder to Leftover User Files, and why (Storage
 *                        S13b, S13d) - what was actually moved is in the report; none for first-time startup and RESTORE
 */
public record Written(WriteReport report, Backup backup, boolean setUpAskedAgain, Leftovers.Plan leftovers) {
    public Written {
        Objects.requireNonNull(report, "report");
        Objects.requireNonNull(backup, "backup");
        Objects.requireNonNull(leftovers, "leftovers");
    }

    /** A write that moved nothing - first-time startup's and RESTORE's. */
    public Written(WriteReport report, Backup backup, boolean setUpAskedAgain) {
        this(report, backup, setUpAskedAgain, Leftovers.Plan.NONE);
    }

    /** A write that left first-time startup as it was - the check's and APPLY's. */
    public Written(WriteReport report, Backup backup, Leftovers.Plan leftovers) {
        this(report, backup, false, leftovers);
    }

    /** A write that left first-time startup as it was, and moved nothing. */
    public Written(WriteReport report, Backup backup) {
        this(report, backup, false, Leftovers.Plan.NONE);
    }
}
