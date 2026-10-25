package elite.intel.bindforge.storage;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * One backup in the backup folder, as its name, its kind folder and its {@code backup-info.txt} describe it.
 *
 * @param zip     the backup's zip
 * @param taken   when it was taken, from the date stamp in its name - never the file's dates
 * @param kind    which of the four kinds, from the folder it is in (file-manager.md, <em>Four kinds of backup</em>)
 * @param before  for a before-changes backup, what it was taken before; {@code null} for the other kinds, or when
 *                its {@code backup-info.txt} does not say
 * @param full    whether every file was read; {@code false} is Partial
 * @param notRead what could not be read, one line each, for a person; empty when {@code full}
 */
public record Backup(Path zip, LocalDateTime taken, Backups.Kind kind, Backups.Before before, boolean full,
                     List<String> notRead) {

    public Backup {
        notRead = List.copyOf(notRead);
    }
}
