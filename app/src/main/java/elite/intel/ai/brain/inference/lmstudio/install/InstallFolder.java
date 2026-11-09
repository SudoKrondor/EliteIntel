package elite.intel.ai.brain.inference.lmstudio.install;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Where a fresh install puts LM Studio.
 * <p>
 * Either LM Studio's own default, with no pointer written, or a folder the commander chose - in which case the app
 * makes its own {@value #SUBFOLDER} folder inside it and points LM Studio there. WHY a folder of its own: uninstall
 * deletes the LM Studio home whole, and the folder the commander picked might be {@code D:\} or a whole games drive.
 *
 * @param home          the LM Studio home the install goes into
 * @param writesPointer whether the install writes {@code ~/.lmstudio-home-pointer} to reach it
 */
public record InstallFolder(Path home, boolean writesPointer) {

    static final String SUBFOLDER = "LMStudio";

    /**
     * LM Studio itself is 2.4 GB on Windows and 3.4 GB on Linux, the model download 6 GB (smoke tests, rounds 2-3).
     */
    public static final long REQUIRED_BYTES = 10L * 1024 * 1024 * 1024;

    /**
     * LM Studio's own home: {@code ~/.lmstudio}, or wherever an existing pointer already says.
     */
    public static InstallFolder lmStudioDefault() {
        return new InstallFolder(LmStudioHome.locate().dir(), false);
    }

    /**
     * A {@value #SUBFOLDER} folder inside {@code chosen}.
     */
    public static InstallFolder inside(Path chosen) {
        return new InstallFolder(chosen.resolve(SUBFOLDER), true);
    }

    /**
     * Free space on the drive the home will be on. The home itself may not exist yet, so this asks of the nearest
     * folder above it that does.
     *
     * @throws IOException when no folder on the way up exists or the drive cannot be asked
     */
    public long usableBytes() throws IOException {
        Path existing = home.toAbsolutePath();
        while (existing != null && !Files.isDirectory(existing)) {
            existing = existing.getParent();
        }
        if (existing == null) throw new IOException("No folder on the way to " + home + " exists");
        return Files.getFileStore(existing).getUsableSpace();
    }
}
