package elite.intel.bindforge.storage;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * One backup in the backup folder, as its name and its {@code backup-info.txt} describe it.
 *
 * @param folder     the backup's folder
 * @param taken      when it was taken, from the date stamp in its name - never the folder's dates
 * @param firstSetup whether it is first-time startup's backup, kept forever (storage.md rule 11)
 * @param full       whether every file was read; {@code false} is Partial
 * @param notRead    what could not be read, one line each, for a person; empty when {@code full}
 */
public record Backup(Path folder, LocalDateTime taken, boolean firstSetup, boolean full, List<String> notRead) {

    public Backup {
        notRead = List.copyOf(notRead);
    }
}
