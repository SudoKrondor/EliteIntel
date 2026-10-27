package elite.intel.bindforge.storage;

import java.util.Objects;

/**
 * What one write through {@link WritePath} did - first-time startup's, RESTORE's, and APPLY's from Storage S4.
 *
 * @param report each place, as the write path reports it
 * @param backup the backup taken before it
 */
public record Written(WriteReport report, Backup backup) {
    public Written {
        Objects.requireNonNull(report, "report");
        Objects.requireNonNull(backup, "backup");
    }
}
