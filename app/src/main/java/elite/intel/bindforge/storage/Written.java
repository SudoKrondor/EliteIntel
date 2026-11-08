package elite.intel.bindforge.storage;

import java.util.List;
import java.util.Objects;

/**
 * What one write through {@link WritePath} did - first-time startup's, RESTORE's, the check's and APPLY's.
 *
 * @param report          each place, as the write path reports it
 * @param backup          the backup taken before it
 * @param setUpAskedAgain whether the write should have marked first-time startup complete and could not, so the next
 *                        click on the BindForge tab offers it again - the user is told (reviews, 2026-10-25 and
 *                        2026-10-27). Always false for APPLY, which never touches the flag
 * @param plain           each plain binds file the check tidied up, and how (Storage S13b) - none for first-time
 *                        startup and RESTORE
 */
public record Written(WriteReport report, Backup backup, boolean setUpAskedAgain, List<PlainBinds.Tidied> plain) {
    public Written {
        Objects.requireNonNull(report, "report");
        Objects.requireNonNull(backup, "backup");
        plain = List.copyOf(plain);
    }

    /** A write that tidied up no plain binds file - first-time startup's and RESTORE's. */
    public Written(WriteReport report, Backup backup, boolean setUpAskedAgain) {
        this(report, backup, setUpAskedAgain, List.of());
    }

    /** A write that left first-time startup as it was - the check's and APPLY's. */
    public Written(WriteReport report, Backup backup, List<PlainBinds.Tidied> plain) {
        this(report, backup, false, plain);
    }

    /** A write that left first-time startup as it was, and tidied up no plain binds file. */
    public Written(WriteReport report, Backup backup) {
        this(report, backup, false, List.of());
    }
}
