package elite.intel.ai.brain.inference.lmstudio.install;

import elite.intel.io.AtomicFiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.UUID;

/**
 * The file the app leaves in an LM Studio home it installed, holding that install's id.
 * <p>
 * WHY: the install record alone cannot tell this PC's LM Studio from another one at the same path. A backup restored
 * onto a PC with the same user name names a folder that exists there too, and an uninstall driven by that record
 * would delete an LM Studio the commander installed themselves. The marker is only ever in a folder the app
 * installed into, so a record counts only while its folder holds a marker with its id.
 */
public final class InstallMarker {

    static final String FILE = ".elite-intel-install";

    private InstallMarker() {
    }

    /**
     * Leaves the marker in {@code homeDir}, which must already exist.
     */
    public static void write(Path homeDir, UUID installId) throws IOException {
        AtomicFiles.write(homeDir.resolve(FILE), installId.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Whether {@code homeDir} holds the marker for {@code installId}. A folder that is missing, unreadable or not a
     * path here holds none.
     */
    public static boolean matches(Path homeDir, UUID installId) {
        try {
            Path marker = homeDir.resolve(FILE);
            return Files.isRegularFile(marker)
                    && Files.readString(marker, StandardCharsets.UTF_8).strip().equals(installId.toString());
        } catch (IOException | InvalidPathException unreadable) {
            return false;
        }
    }
}
