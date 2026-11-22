package elite.intel.ai.brain.inference.lmstudio.install;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import elite.intel.io.AtomicFiles;
import elite.intel.util.OsDetector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
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
     * Where LM Studio records the programs {@code lms} starts: llmster's (what LM Studio's install script installs)
     * and the desktop app's. Each is JSON whose {@code path} names the program. {@code lms daemon up} goes by these
     * and fails with "no valid installation could be found" when none names a program that is there.
     */
    static final List<String> INSTALL_RECORDS = List.of("llmster-install-location.json", "app-install-location.json");

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
     * The {@code lms} command, when one is in {@code bin} - whether or not LM Studio is still installed. Use
     * {@link #installedLms()} to ask whether there is an LM Studio {@code lms} can start.
     */
    public Optional<Path> lmsCommand() {
        String name = OsDetector.getOs() == OsDetector.OS.WINDOWS ? "lms.exe" : "lms";
        Path command = dir.resolve("bin").resolve(name);
        return Files.isRegularFile(command) ? Optional.of(command) : Optional.empty();
    }

    /**
     * The {@code lms} command, when LM Studio is really installed here: {@code lms} in {@code bin} and an
     * {@linkplain #INSTALL_RECORDS install record} naming a program that is there.
     * <p>
     * WHY not {@code lms} alone: removing LM Studio leaves its home behind, {@code bin/lms.exe} included (a Windows
     * tester's support bundle, 2026-11-10), and that {@code lms} then fails to start anything.
     */
    public Optional<Path> installedLms() {
        return lmsCommand().filter(lms -> !installedPrograms().isEmpty());
    }

    /**
     * The programs LM Studio's install records name that are still there; empty when LM Studio was removed.
     */
    public List<Path> installedPrograms() {
        return INSTALL_RECORDS.stream()
                .map(name -> recordedProgram(dir.resolve(".internal").resolve(name)))
                .flatMap(Optional::stream)
                .filter(Files::exists)
                .toList();
    }

    private static Optional<Path> recordedProgram(Path record) {
        if (!Files.isRegularFile(record)) return Optional.empty();
        try {
            JsonElement path = JsonParser.parseString(Files.readString(record, StandardCharsets.UTF_8))
                    .getAsJsonObject().get("path");
            return path != null && path.isJsonPrimitive() ? Optional.of(Path.of(path.getAsString())) : Optional.empty();
        } catch (IOException | JsonParseException | IllegalStateException | InvalidPathException unreadable) {
            // WHY no install: lms reads the same file, and one it cannot follow starts nothing either.
            log.warn("Could not read LM Studio's install record {}: {}", record, unreadable.getMessage());
            return Optional.empty();
        }
    }
}
