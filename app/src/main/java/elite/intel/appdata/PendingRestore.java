package elite.intel.appdata;

import elite.intel.util.AppPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

/**
 * The second half of a restore: at startup, before the key is loaded or the database opens, swap in the backup
 * {@link AppDataRestore} left waiting.
 * <p>
 * What the backup replaces is moved into a {@value AppDataArchive#SAFETY_FOLDER_PREFIX} folder, never deleted
 * outright, and the newest {@value #SAFETY_FOLDERS_KEPT} such folders are kept. If any move fails, every move
 * already made is undone and the app starts on the data it had.
 */
public final class PendingRestore {

    private static final Logger log = LogManager.getLogger(PendingRestore.class);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    static final int SAFETY_FOLDERS_KEPT = 2;

    /**
     * What the start found.
     */
    public enum Outcome {
        /**
         * No restore was waiting.
         */
        NONE,
        RESTORED,
        /**
         * A restore was waiting and could not be swapped in; the app runs on the data it had.
         */
        FAILED
    }

    private PendingRestore() {
    }

    /**
     * Swaps in a waiting restore in the app's own data folder, if there is one.
     */
    public static Outcome applyAtStartup() {
        try {
            return applyIfPresent(AppPaths.getAppDataDir(), Path.of(AppPaths.getSecretKeyFile()));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot find the app data folder", e);
        }
    }

    /**
     * Swaps in a waiting restore, if there is one.
     *
     * @param keyFile the app's {@code secret.key}, replaced when the backup carried its key
     */
    public static Outcome applyIfPresent(Path dataDir, Path keyFile) {
        Path pending = dataDir.resolve(AppDataArchive.PENDING_FOLDER);
        if (!Files.isDirectory(pending)) return Outcome.NONE;

        Path safety = dataDir.resolve(AppDataArchive.SAFETY_FOLDER_PREFIX + LocalDateTime.now().format(STAMP));
        Deque<Move> done = new ArrayDeque<>();
        try {
            Files.createDirectories(safety);
            for (String part : restoredParts()) {
                swap(pending.resolve(part), dataDir.resolve(part), safety.resolve(part), done);
            }
            swap(pending.resolve(AppDataArchive.KEY_FILE), keyFile, safety.resolve(AppDataArchive.KEY_FILE), done);
        } catch (IOException e) {
            log.error("Restore could not be swapped in; going back to the data the app had", e);
            undo(done);
            discard(pending);
            return Outcome.FAILED;
        }
        discard(pending);
        pruneSafetyFolders(dataDir);
        log.info("Restored settings and data; what they replaced is in {}", safety);
        return Outcome.RESTORED;
    }

    private static List<String> restoredParts() {
        List<String> parts = new ArrayList<>();
        parts.add(AppDataArchive.DB_FOLDER);
        parts.addAll(AppDataArchive.FOLDERS);
        return parts;
    }

    /**
     * Moves {@code live} aside and {@code staged} into its place. A part the backup does not have leaves the live
     * one where it is - a key left out of the backup keeps this computer's key.
     */
    private static void swap(Path staged, Path live, Path aside, Deque<Move> done) throws IOException {
        if (!Files.exists(staged)) return;
        if (Files.exists(live)) {
            Files.createDirectories(aside.getParent());
            Files.move(live, aside);
            done.push(new Move(live, aside));
        }
        Files.createDirectories(live.getParent());
        Files.move(staged, live);
        done.push(new Move(staged, live));
    }

    private static void undo(Deque<Move> done) {
        while (!done.isEmpty()) {
            Move move = done.pop();
            try {
                Files.move(move.to(), move.from());
            } catch (IOException e) {
                log.error("Could not move {} back to {}; it is still at the second path", move.to(), move.from(), e);
            }
        }
    }

    private static void discard(Path pending) {
        try {
            AppDataBackup.deleteTree(pending);
        } catch (IOException e) {
            log.warn("Could not remove the restore folder {}", pending, e);
        }
    }

    private static void pruneSafetyFolders(Path dataDir) {
        try (Stream<Path> entries = Files.list(dataDir)) {
            List<Path> stale = entries
                    .filter(Files::isDirectory)
                    .filter(dir -> dir.getFileName().toString().startsWith(AppDataArchive.SAFETY_FOLDER_PREFIX))
                    .sorted(Comparator.comparing((Path dir) -> dir.getFileName().toString()).reversed())
                    .skip(SAFETY_FOLDERS_KEPT)
                    .toList();
            for (Path dir : stale) {
                AppDataBackup.deleteTree(dir);
            }
        } catch (IOException e) {
            log.warn("Could not prune old restore safety folders in {}", dataDir, e);
        }
    }

    private record Move(Path from, Path to) {
    }
}
