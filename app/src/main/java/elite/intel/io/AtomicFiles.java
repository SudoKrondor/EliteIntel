package elite.intel.io;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.UUID;

/**
 * Replaces a file so that a reader - another process included - sees either the old content or the new,
 * never a half-written file.
 * <p>
 * The new content goes to a uniquely named, hidden temp file beside the target, is flushed to disk, and is
 * then renamed over the target. The temp file is removed if any step fails. The target's parent directory
 * must already exist.
 */
public final class AtomicFiles {

    private AtomicFiles() {
    }

    /**
     * Replaces {@code target} with {@code content}, creating it if it does not exist.
     */
    public static void write(Path target, byte[] content) throws IOException {
        replace(target, tmp -> Files.write(tmp, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE));
    }

    /**
     * Replaces {@code target} with a copy of {@code source}, keeping the source's attributes (its
     * last-modified time included) the way {@link StandardCopyOption#COPY_ATTRIBUTES} does.
     */
    public static void copy(Path source, Path target) throws IOException {
        replace(target, tmp -> Files.copy(source, tmp, StandardCopyOption.COPY_ATTRIBUTES));
    }

    private static void replace(Path target, TempFill fill) throws IOException {
        Path tmp = tempSibling(target);
        try {
            fill.writeTo(tmp);
            flushToDisk(tmp);
            move(tmp, target);
        } catch (IOException e) {
            deleteQuietly(tmp, e);
            throw e;
        }
    }

    // WHY: a unique name means two writers on the same target never share a temp file, and CREATE_NEW
    // (in the fills) never reuses one a crash left behind. The leading dot keeps it out of a casual listing,
    // and the .tmp ending keeps it from matching a suffix a folder's reader looks for (.binds, .json).
    private static Path tempSibling(Path target) {
        return target.resolveSibling("." + target.getFileName() + ".elite-intel-" + UUID.randomUUID() + ".tmp");
    }

    // WHY: without this, a power loss after the rename can leave the target empty on file systems that
    // reorder the rename ahead of the data. Opening for WRITE and writing nothing leaves the timestamps alone.
    private static void flushToDisk(Path file) throws IOException {
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
            channel.force(true);
        }
    }

    private static void deleteQuietly(Path tmp, IOException cause) {
        try {
            Files.deleteIfExists(tmp);
        } catch (IOException cleanupFailure) {
            cause.addSuppressed(cleanupFailure);
        }
    }

    // WHY: some network shares and FAT volumes refuse ATOMIC_MOVE. A plain replacing move there is still
    // far safer than writing the target in place, so this degrades rather than failing the save.
    private static void move(Path tmp, Path target) throws IOException {
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @FunctionalInterface
    private interface TempFill {
        void writeTo(Path tmp) throws IOException;
    }
}
