package elite.intel.appdata;

import elite.intel.io.FileTrees;
import elite.intel.util.AppPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryNotEmptyException;
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
 * outright - except in {@link AppDataArchive#MERGED_FOLDERS}, where the backup's files are added and nothing is
 * replaced - and the newest {@value #SAFETY_FOLDERS_KEPT} such folders are kept. If any move fails, every move
 * already made is undone, the app starts on the data it had, and the restore waits for the next start (up to
 * {@value #MAX_ATTEMPTS} starts).
 */
public final class PendingRestore {

    private static final Logger log = LogManager.getLogger(PendingRestore.class);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    static final int SAFETY_FOLDERS_KEPT = 2;
    /**
     * Starts a restore may fail on before it is discarded.
     */
    static final int MAX_ATTEMPTS = 3;
    /**
     * Inside the pending folder: how many starts failed to swap it in. Not a restored part, so it is never moved.
     */
    static final String ATTEMPTS_FILE = "swap-attempts";

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
         * A restore was waiting and could not be swapped in this time; the app runs on the data it had, and the
         * restore stays waiting for the next start.
         */
        RETRYING,
        /**
         * A restore was waiting and could not be swapped in, and is discarded; the app runs on the data it had.
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
                if (AppDataArchive.MERGED_FOLDERS.contains(part)) {
                    merge(pending.resolve(part), dataDir.resolve(part), done);
                } else {
                    swap(pending.resolve(part), dataDir.resolve(part), safety.resolve(part), done);
                }
            }
            swap(pending.resolve(AppDataArchive.KEY_FILE), keyFile, safety.resolve(AppDataArchive.KEY_FILE), done);
        } catch (IOException e) {
            return giveBack(pending, safety, done, e);
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

    /**
     * Moves each file of {@code staged} that {@code live} does not have into its place, and leaves every file
     * {@code live} has where it is ({@link AppDataArchive#MERGED_FOLDERS}). Nothing is moved aside, so the undo takes
     * back only what was added.
     */
    private static void merge(Path staged, Path live, Deque<Move> done) throws IOException {
        if (!Files.isDirectory(staged)) return;
        List<Path> files;
        try (Stream<Path> walk = Files.walk(staged)) {
            files = walk.filter(Files::isRegularFile).sorted().toList();
        }
        for (Path file : files) {
            Path target = live.resolve(staged.relativize(file).toString());
            if (Files.exists(target)) continue;
            Files.createDirectories(target.getParent());
            Files.move(file, target);
            done.push(new Move(file, target));
        }
    }

    /**
     * Undoes a swap that failed part way and decides whether the restore waits for another start.
     * <p>
     * WHY kept rather than thrown away: the usual cause is a file the previous Elite-Intel still holds open - on
     * Windows it cannot be moved, and that copy can outlive the relaunch's wait while its exit stops LM Studio. Thrown
     * away, the commander would have to choose the zip and type the password again for a lock that is gone a minute
     * later. A restore that fails {@value #MAX_ATTEMPTS} starts running, or whose undo did not finish (part of it is
     * no longer where a retry would look), is discarded.
     */
    private static Outcome giveBack(Path pending, Path safety, Deque<Move> done, IOException cause) {
        boolean undone = undo(done);
        removeIfEmpty(safety);
        int attempts = countAttempt(pending);
        if (undone && attempts < MAX_ATTEMPTS) {
            log.error("Restore could not be swapped in (attempt {} of {}); the app runs on the data it had and the "
                    + "restore is tried again at the next start", attempts, MAX_ATTEMPTS, cause);
            return Outcome.RETRYING;
        }
        log.error("Restore could not be swapped in (attempt {} of {}, undo {}); the app runs on the data it had and "
                + "the restore is discarded", attempts, MAX_ATTEMPTS, undone ? "complete" : "incomplete", cause);
        discard(pending);
        return Outcome.FAILED;
    }

    /**
     * Records one more failed start in {@code pending}, returning the count so far.
     * <p>
     * WHY a counter that cannot be written counts as the last attempt: without it nothing bounds the retries, and a
     * restore that can never be swapped in would ask at every start.
     */
    private static int countAttempt(Path pending) {
        Path file = pending.resolve(ATTEMPTS_FILE);
        try {
            int attempts = Files.isRegularFile(file) ? Integer.parseInt(Files.readString(file).strip()) + 1 : 1;
            Files.writeString(file, String.valueOf(attempts));
            return attempts;
        } catch (IOException | NumberFormatException e) {
            log.error("Could not count the restore's failed attempts in {}", file, e);
            return MAX_ATTEMPTS;
        }
    }

    /**
     * WHY: a failed swap leaves its safety folder empty, and each one would push a real safety folder out of the
     * {@value #SAFETY_FOLDERS_KEPT} kept.
     */
    private static void removeIfEmpty(Path safety) {
        try {
            Files.deleteIfExists(safety);
        } catch (DirectoryNotEmptyException kept) {
            log.warn("Restore safety folder {} still holds data the undo could not move back", safety);
        } catch (IOException e) {
            log.warn("Could not remove the empty restore safety folder {}", safety, e);
        }
    }

    /**
     * @return true when every move was taken back
     */
    private static boolean undo(Deque<Move> done) {
        boolean complete = true;
        while (!done.isEmpty()) {
            Move move = done.pop();
            try {
                Files.move(move.to(), move.from());
            } catch (IOException e) {
                complete = false;
                log.error("Could not move {} back to {}; it is still at the second path", move.to(), move.from(), e);
            }
        }
        return complete;
    }

    private static void discard(Path pending) {
        try {
            FileTrees.delete(pending);
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
                FileTrees.delete(dir);
            }
        } catch (IOException e) {
            log.warn("Could not prune old restore safety folders in {}", dataDir, e);
        }
    }

    private record Move(Path from, Path to) {
    }
}
