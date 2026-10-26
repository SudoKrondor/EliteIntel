package elite.intel.bindforge.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;

/**
 * Windows' read-only flag on a live file - which players set to stop the game resetting a binds file (file-manager.md,
 * <em>The read-only flag</em>). Where the file system has no such flag, a file never has it.
 */
final class ReadOnlyFlag {

    private ReadOnlyFlag() {
    }

    /** Whether {@code file} is there and carries the flag. */
    static boolean isSet(Path file) throws IOException {
        DosFileAttributeView view = viewOf(file);
        return view != null && Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                && view.readAttributes().isReadOnly();
    }

    /** Sets or clears the flag; nothing where the file system has none. */
    static void set(Path file, boolean readOnly) throws IOException {
        DosFileAttributeView view = viewOf(file);
        if (view != null) view.setReadOnly(readOnly);
    }

    private static DosFileAttributeView viewOf(Path file) {
        // WHY: only where the store really keeps DOS attributes. On Linux the view can exist and be emulated, which
        // is not the flag a Windows player sets.
        try {
            if (!Files.getFileStore(file.getParent()).supportsFileAttributeView("dos")) return null;
        } catch (IOException e) {
            return null;
        }
        return Files.getFileAttributeView(file, DosFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
    }
}
