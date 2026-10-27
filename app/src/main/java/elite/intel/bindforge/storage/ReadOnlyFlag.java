package elite.intel.bindforge.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Locale;
import java.util.Set;

/**
 * Whether a live file is protected from being written - which players do to stop the game resetting a binds file
 * (file-manager.md, <em>The read-only flag</em>):
 * <ul>
 *     <li><strong>on Windows</strong>, the read-only flag;</li>
 *     <li><strong>elsewhere</strong> - Linux, where the game runs under Proton - the owner's write permission, which is
 *     what Wine shows as read-only (Alan, 2026-10-25, from the S3 review).</li>
 * </ul>
 * Where the file system has neither, a file is never protected.
 */
final class ReadOnlyFlag {

    /**
     * Decided by the system, not by which views the file store offers: on Linux the JDK offers a "dos" view wherever
     * extended attributes are enabled, emulated in an attribute nothing else reads (review, 2026-10-25).
     */
    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");

    private ReadOnlyFlag() {
    }

    /** Whether {@code file} is there and protected. */
    static boolean isSet(Path file) throws IOException {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) return false;
        if (WINDOWS) {
            DosFileAttributeView dos = Files.getFileAttributeView(file, DosFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
            return dos != null && dos.readAttributes().isReadOnly();
        }
        PosixFileAttributeView posix = Files.getFileAttributeView(file, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        return posix != null && !posix.readAttributes().permissions().contains(PosixFilePermission.OWNER_WRITE);
    }

    /** Protects {@code file}, or lifts the protection; nothing where the file system has none. */
    static void set(Path file, boolean readOnly) throws IOException {
        if (WINDOWS) {
            DosFileAttributeView dos = Files.getFileAttributeView(file, DosFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
            if (dos != null) dos.setReadOnly(readOnly);
            return;
        }
        PosixFileAttributeView posix = Files.getFileAttributeView(file, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        if (posix == null) return;
        Set<PosixFilePermission> permissions = posix.readAttributes().permissions();
        if (readOnly) {
            permissions.remove(PosixFilePermission.OWNER_WRITE);
        } else {
            permissions.add(PosixFilePermission.OWNER_WRITE);
        }
        posix.setPermissions(permissions);
    }
}
