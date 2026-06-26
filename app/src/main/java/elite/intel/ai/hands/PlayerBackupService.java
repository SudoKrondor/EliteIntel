package elite.intel.ai.hands;

import elite.intel.eventbus.UiBus;
import elite.intel.session.PlayerSession;
import elite.intel.ui.event.BindingsUpdatedEvent;
import elite.intel.util.AppPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Creates, lists, and restores on-demand, user-facing snapshots of every {@code .binds} file in
 * the bindings directory plus {@code StartPreset.*.start}, written to {@code playerbackups}
 * (see {@link AppPaths#getPlayerBackupsDir()}). Deliberately separate from the internal,
 * per-Apply {@link BindingsBackupService}/{@link BindingsApplyService} mechanism, which is an
 * apply-pipeline safety net rather than a user-triggered feature.
 * <p>
 * Restore has two targets sharing the same first step (loading the backup's file for the active
 * preset into the working copy as the new draft): {@link #restoreToWorkingCopy} stops there;
 * {@link #restoreToLive} continues into the existing safe-apply pipeline so a live restore still
 * gets that pipeline's own conflict-check and pre-write backup, rather than a separate,
 * less-safe direct write to the game directory.
 */
public class PlayerBackupService {

    private static final Logger log = LogManager.getLogger(PlayerBackupService.class);
    private static final DateTimeFormatter BACKUP_FOLDER_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private static volatile PlayerBackupService instance;

    private final BindingsLoader bindingsLoader;
    private final BindingsWorkingCopyRepository workingCopyRepo;
    private final BindingsApplyService applyService;
    private final Clock clock;
    /** Overrides {@link AppPaths#getPlayerBackupsDir()} in tests; {@code null} in production. */
    private final Path baseDirOverride;

    private PlayerBackupService() {
        this(new BindingsLoader(), new BindingsWorkingCopyRepository(), new BindingsApplyService(),
                Clock.systemDefaultZone(), null);
    }

    PlayerBackupService(
            BindingsLoader bindingsLoader,
            BindingsWorkingCopyRepository workingCopyRepo,
            BindingsApplyService applyService,
            Clock clock,
            Path baseDirOverride
    ) {
        this.bindingsLoader = bindingsLoader;
        this.workingCopyRepo = workingCopyRepo;
        this.applyService = applyService;
        this.clock = clock;
        this.baseDirOverride = baseDirOverride;
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
     * Sweeps every {@code .binds} file in the bindings directory plus {@code StartPreset.*.start}
     * into a new timestamped folder under {@code playerbackups}, with real filenames intact.
     *
     * @return the folder the backup was written to
     * @throws IOException if the bindings directory has nothing to back up, or the copy fails
     */
    public Path createBackup() throws IOException {
        return createBackup(PlayerSession.getInstance().getBindingsDir());
    }

    /** Test seam: same as {@link #createBackup()} but with an explicit bindings directory. */
    Path createBackup(Path bindingsDir) throws IOException {
        List<Path> filesToBackup = new ArrayList<>(bindingsLoader.listAllBindsFiles(bindingsDir));
        bindingsLoader.findStartPresetFile(bindingsDir).ifPresent(filesToBackup::add);
        if (filesToBackup.isEmpty()) {
            throw new IOException("No .binds or StartPreset files found in " + bindingsDir);
        }

        Path backupFolder = uniqueBackupFolder(resolvePlayerBackupsDir());
        Files.createDirectories(backupFolder);
        for (Path file : filesToBackup) {
            Files.copy(file, backupFolder.resolve(file.getFileName()), StandardCopyOption.COPY_ATTRIBUTES);
        }

        log.info("Created player backup at {} ({} files)", backupFolder, filesToBackup.size());
        return backupFolder;
    }

    /** Lists existing backups, newest first. */
    public List<PlayerBackup> listBackups() throws IOException {
        Path backupRoot = resolvePlayerBackupsDir();
        try (var stream = Files.list(backupRoot)) {
            return stream
                    .filter(Files::isDirectory)
                    .map(this::toPlayerBackup)
                    .sorted(Comparator.comparing(PlayerBackup::timestamp).reversed())
                    .toList();
        }
    }

    /**
     * Loads {@code presetFileName} from {@code backupFolder} into the current working copy,
     * making it the new draft - the same starting point as any other edit-in-progress.
     * Publishes {@link BindingsUpdatedEvent} so the Binding Profile tab reloads and reflects it.
     *
     * @throws IOException if the backup has no file for that preset, or the write fails
     */
    public void restoreToWorkingCopy(Path backupFolder, String presetFileName) throws IOException {
        Path backupFile = backupFolder.resolve(presetFileName);
        if (!Files.exists(backupFile)) {
            throw new IOException("Backup " + backupFolder.getFileName() + " has no file named " + presetFileName);
        }
        String content = Files.readString(backupFile, StandardCharsets.UTF_8);
        workingCopyRepo.save(presetFileName, content);
        log.info("Restored '{}' from backup {} into the working copy", presetFileName, backupFolder.getFileName());
        UiBus.publish(new BindingsUpdatedEvent());
    }

    /**
     * Same first step as {@link #restoreToWorkingCopy}, then immediately applies the restored
     * draft to the game directory via the existing safe-apply pipeline - the same
     * conflict-check and pre-write backup any other Apply gets.
     *
     * @return the path of the apply pipeline's own pre-write backup, or {@code null} if the game file did not exist
     */
    public Path restoreToLive(Path backupFolder, String presetFileName, Path gameBindsFile)
            throws IOException, BindingsApplyException {
        restoreToWorkingCopy(backupFolder, presetFileName);
        return applyService.apply(presetFileName, gameBindsFile);
    }

    private PlayerBackup toPlayerBackup(Path folder) {
        List<String> fileNames;
        try (var stream = Files.list(folder)) {
            fileNames = stream
                    .map(p -> p.getFileName().toString())
                    .sorted(String::compareToIgnoreCase)
                    .toList();
        } catch (IOException e) {
            log.warn("Could not list backup contents in {}: {}", folder, e.getMessage());
            fileNames = List.of();
        }
        return new PlayerBackup(folder, folder.getFileName().toString(), fileNames);
    }

    private Path resolvePlayerBackupsDir() throws IOException {
        if (baseDirOverride != null) {
            Files.createDirectories(baseDirOverride);
            return baseDirOverride;
        }
        return AppPaths.getPlayerBackupsDir();
    }

    /** Appends a numeric suffix on collision (two backups requested within the same second). */
    private Path uniqueBackupFolder(Path backupRoot) {
        String timestamp = ZonedDateTime.now(clock).format(BACKUP_FOLDER_TIMESTAMP);
        Path candidate = backupRoot.resolve(timestamp);
        for (int attempt = 1; Files.exists(candidate); attempt++) {
            candidate = backupRoot.resolve(timestamp + "-" + attempt);
        }
        return candidate;
    }

    /** One backup folder's worth of display info: its timestamp label and the files it contains. */
    public record PlayerBackup(Path folder, String timestamp, List<String> fileNames) {
    }
}
