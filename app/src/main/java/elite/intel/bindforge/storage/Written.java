package elite.intel.bindforge.storage;

import java.util.Objects;

/**
 * What one write through {@link WritePath} did - first-time startup's, RESTORE's and APPLY's.
 *
 * @param report          each place, as the write path reports it
 * @param backup          the backup taken before it
 * @param setUpAskedAgain whether the write should have marked first-time startup complete and could not, so the next
 *                        click on the BindForge tab offers it again - the user is told (reviews, 2026-10-25 and
 *                        2026-10-27). Always false for APPLY, which never touches the flag
 */
public record Written(WriteReport report, Backup backup, boolean setUpAskedAgain) {
    public Written {
        Objects.requireNonNull(report, "report");
        Objects.requireNonNull(backup, "backup");
    }

    /** A write that left first-time startup as it was - APPLY's. */
    public Written(WriteReport report, Backup backup) {
        this(report, backup, false);
    }
}
