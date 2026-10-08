package elite.intel.bindforge.devicefiles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * Writes a file that must not already exist: a flushed temp file beside it, then published under the target's
 * name by a step that refuses to replace. A reader sees no file or the whole file, and a file that appeared in the
 * meantime is kept.
 * <p>
 * The create-only sibling of {@code AtomicFiles.write}, which renames <em>over</em> the target - right for Apply,
 * wrong for a write whose one promise is that it never overwrites. It lives here rather than in
 * {@code elite.intel.io} because that package is shared with V1.1; the generated {@code .buttonMap} (A8) and
 * onboarding's writes (A6b) are its callers.
 */
final class CreateOnlyFiles {

    private static final Logger log = LogManager.getLogger(CreateOnlyFiles.class);

    private CreateOnlyFiles() {
    }

    /**
     * @return {@code false} when the target was already there, in which case nothing was written
     * @throws IOException if the file could not be written - including when the writing thread is interrupted,
     *                     in which case the temp file is still removed
     */
    static boolean createNew(Path target, byte[] content) throws IOException {
        Files.createDirectories(target.getParent());
        Path temp = tempSibling(target);
        IOException failure = null;
        try {
            writeFlushed(temp, content);
            return publish(temp, target);
        } catch (IOException e) {
            failure = e;
            throw e;
        } finally {
            deleteTemp(temp, failure);
        }
    }

    // WHY: named as AtomicFiles names its temp files, so a leftover in a game folder is recognisably Elite-Intel's.
    // The leading dot keeps it out of a casual listing, and the .tmp ending keeps it from matching .buttonMap.
    private static Path tempSibling(Path target) {
        return target.resolveSibling("." + target.getFileName() + ".elite-intel-" + UUID.randomUUID() + ".tmp");
    }

    // WHY: force goes through a FileChannel, which is interruptible - a write cut short by the service stopping
    // throws ClosedByInterruptException, and the caller's finally removes the temp file.
    private static void writeFlushed(Path file, byte[] content) throws IOException {
        Files.write(file, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
            channel.force(true);
        }
    }

    // WHY: a hard link is created by link(2) on Linux and CreateHardLink on Windows, and both fail atomically when
    // the target exists - a plain move only does that on Windows, while the JDK's Linux move checks and then
    // renames, which replaces. Some volumes (FAT, some shares) refuse hard links; there a plain move is used,
    // which refuses to replace on Windows and narrows the window to a check-then-rename on Linux.
    private static boolean publish(Path temp, Path target) throws IOException {
        try {
            Files.createLink(target, temp);
            return true;
        } catch (FileAlreadyExistsException e) {
            return false;
        } catch (IOException | UnsupportedOperationException linkRefused) {
            log.debug("Hard link refused for {}, falling back to a plain move: {}", target,
                    linkRefused.getMessage());
        }
        try {
            Files.move(temp, target);
            return true;
        } catch (FileAlreadyExistsException e) {
            return false;
        }
    }

    /**
     * Removes the temp file without letting its failure replace the outcome: attached to the real failure when
     * there is one, logged otherwise - the file was published, or was already there, either way.
     */
    private static void deleteTemp(Path temp, IOException failure) {
        try {
            Files.deleteIfExists(temp);
        } catch (IOException cleanupFailure) {
            if (failure != null) {
                failure.addSuppressed(cleanupFailure);
            } else {
                log.warn("Could not remove the temp file {}: {}", temp, cleanupFailure.getMessage());
            }
        }
    }
}
