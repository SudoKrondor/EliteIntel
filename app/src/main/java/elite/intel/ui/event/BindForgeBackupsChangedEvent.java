package elite.intel.ui.event;

/**
 * A backup was taken, deleted, or pruned by retention - by any caller: APPLY, RESTORE, the check, first-time startup,
 * the daily backup, Backup Now or Delete - so Player Backups reads the backup folder again (Alan, 2026-11-14).
 * Published by {@code bindforge.storage.Backups}, on whichever thread made the change.
 */
public record BindForgeBackupsChangedEvent() {
}
