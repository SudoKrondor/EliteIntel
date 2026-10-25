package elite.intel.bindforge.io;

import elite.intel.bindforge.storage.Backup;
import elite.intel.bindforge.storage.Backups;
import elite.intel.eventbus.UiBus;
import elite.intel.ui.event.BindingsUpdatedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Restores one {@code .binds} file from a backup - Restore to Editing Slot and Restore to Live, until RESTORE
 * replaces both (Storage S3).
 * <p>
 * Taking, listing, pruning and reading backups is {@link Backups}. A backup is a zip since Storage S1b (2026-10-24),
 * holding its {@code .binds} under {@code Bindings\}; old folder backups are no longer listed, so are not read here.
 * <p>
 * Restore has two targets sharing the same first step (loading the backup's file for the active preset into the
 * working copy as the new draft): {@link #restoreToWorkingCopy} stops there; {@link #restoreToLive} continues into the
 * existing safe-apply pipeline so a live restore still gets that pipeline's own conflict-check and pre-write backup,
 * rather than a separate, less-safe direct write to the game directory.
 */
public class PlayerBackupService {

    private static final Logger log = LogManager.getLogger(PlayerBackupService.class);

    private static volatile PlayerBackupService instance;

    private final BindingsWorkingCopyRepository workingCopyRepo;
    private final BindingsApplyService applyService;

    private PlayerBackupService() {
        this(new BindingsWorkingCopyRepository(), new BindingsApplyService());
    }

    /** Test seam. */
    PlayerBackupService(BindingsWorkingCopyRepository workingCopyRepo, BindingsApplyService applyService) {
        this.workingCopyRepo = workingCopyRepo;
        this.applyService = applyService;
    }

    public static PlayerBackupService getInstance() {
        if (instance == null) {
            synchronized (PlayerBackupService.class) {
                if (instance == null) {
                    instance = new PlayerBackupService();
                }
            }
        }
        return instance;
    }

    /**
     * Loads {@code presetFileName} from {@code backup} into the current working copy,
     * making it the new draft - the same starting point as any other edit-in-progress.
     * Publishes {@link BindingsUpdatedEvent} so the Binding Profile tab reloads and reflects it.
     *
     * @throws IOException if the backup has no file for that preset, or the write fails
     */
    public void restoreToWorkingCopy(Backup backup, String presetFileName) throws IOException {
        String content = Backups.readBindingsFile(backup, presetFileName);
        workingCopyRepo.save(presetFileName, content);
        log.info("Restored '{}' from backup {} into the working copy", presetFileName, backup.zip());
        UiBus.publish(new BindingsUpdatedEvent());
    }

    /**
     * Same first step as {@link #restoreToWorkingCopy}, then immediately applies the restored
     * draft to the game directory via the existing safe-apply pipeline - the same
     * conflict-check and pre-write backup any other Apply gets.
     *
     * @return the path of the apply pipeline's own pre-write backup, {@code null} if the game
     * file did not exist, or {@code null} if the restored content is already identical
     * to the current game file (no-op)
     */
    public Path restoreToLive(Backup backup, String presetFileName, Path gameBindsFile)
            throws IOException, BindingsApplyException {
        restoreToWorkingCopy(backup, presetFileName);
        workingCopyRepo.recordGameFileBaseline(presetFileName, gameBindsFile);
        Path appliedBackup = applyService.apply(presetFileName, gameBindsFile);
        // WHY: restoreToWorkingCopy() published BindingsUpdatedEvent while the content was still
        // a draft. Publish again after apply() completes so BindingProfilePanel recomputes the
        // sync badge (DRAFT -> SYNCED). Published unconditionally because both the applied and
        // no-op paths leave hasUnappliedDraft() returning false. Mirrors parseAndUpdateBindings().
        UiBus.publish(new BindingsUpdatedEvent());
        return appliedBackup;
    }
}
