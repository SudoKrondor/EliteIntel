package elite.intel.ai.brain.inference.lmstudio.install;

import elite.intel.io.AtomicFiles;
import elite.intel.util.OsDetector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Where LM Studio keeps itself on this PC: the folder named in {@code ~/.lmstudio-home-pointer} when that file
 * exists, else {@code ~/.lmstudio}. The desktop app and the headless llmster both live there, and both put their
 * {@code lms} command in its {@code bin} folder.
 * <p>
 * The pointer is LM Studio's own way of moving its home: its install scripts read it, and smoke testing on both
 * platforms showed the daemon and every downloaded model follow it too.
 */
public record LmStudioHome(Path dir) {

    private static final Logger log = LogManager.getLogger(LmStudioHome.class);

    static final String POINTER_FILE = ".lmstudio-home-pointer";
    static final String DEFAULT_DIR = ".lmstudio";

    /**
     * LM Studio's home for the user running the app.
     */
    public static LmStudioHome locate() {
        return locate(Path.of(System.getProperty("user.home")));
    }

    static LmStudioHome locate(Path userHome) {
        Path pointer = userHome.resolve(POINTER_FILE);
        if (!Files.isRegularFile(pointer)) {
            return new LmStudioHome(userHome.resolve(DEFAULT_DIR));
        }
        try {
            String named = Files.readString(pointer, StandardCharsets.UTF_8).strip();
            if (!named.isEmpty()) {
                return new LmStudioHome(Path.of(named));
            }
        } catch (IOException | InvalidPathException unreadable) {
            // WHY carry on with the default: this only decides what the setup button offers, and a pointer LM Studio
            // itself could not follow leaves no usable LM Studio to find either way.
            log.warn("Could not read LM Studio's home pointer {}: {}", pointer, unreadable.getMessage());
        }
        return new LmStudioHome(userHome.resolve(DEFAULT_DIR));
    }

    /**
     * Points LM Studio at {@code home} for the user running the app, before LM Studio is installed there.
     */
    public static void writePointer(Path home) throws IOException {
        writePointer(Path.of(System.getProperty("user.home")), home);
    }

    static void writePointer(Path userHome, Path home) throws IOException {
        // WHY no newline: written exactly as smoke testing proved LM Studio's installers read it.
        AtomicFiles.write(userHome.resolve(POINTER_FILE), home.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Removes the pointer when it still names {@code home}, so LM Studio's home goes back to {@code ~/.lmstudio}. One
     * naming somewhere else is the commander's own choice since, and is left alone.
     */
    static void removePointerTo(Path userHome, Path home) throws IOException {
        if (!Files.isRegularFile(userHome.resolve(POINTER_FILE))) return;
        if (!locate(userHome).dir().equals(home)) return;
        Files.delete(userHome.resolve(POINTER_FILE));
    }

    /**
     * The {@code lms} command, when LM Studio is installed here.
     */
    public Optional<Path> lmsCommand() {
        String name = OsDetector.getOs() == OsDetector.OS.WINDOWS ? "lms.exe" : "lms";
        Path command = dir.resolve("bin").resolve(name);
        return Files.isRegularFile(command) ? Optional.of(command) : Optional.empty();
    }
}
