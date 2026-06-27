package elite.intel.gameapi;

import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Validates that a configured data directory actually exists and contains the files we expect to
 * process - journal logs for {@link DirectoryKind#JOURNAL} or key-binding presets for
 * {@link DirectoryKind#BINDINGS}.
 * <p>
 * The user can point either directory at the wrong place (or empty it out), so this guard is used in
 * two situations:
 * <ol>
 *     <li>when the user picks a directory in the UI, and</li>
 *     <li>when the {@code JournalParser} / {@code BindingsMonitor} services start against the
 *     configured directory.</li>
 * </ol>
 * On failure it raises a {@link MissionCriticalAnnouncementEvent}, which is always vocalized
 * regardless of speech settings, so the commander is told immediately. Warning text is localized via
 * the gui i18n bundle.
 */
public final class DataDirectoryValidator {

    private static final Logger log = LogManager.getLogger(DataDirectoryValidator.class);

    /**
     * The kind of data directory, carrying the file suffix we scan for and its i18n key fragment.
     */
    public enum DirectoryKind {
        JOURNAL(".log", "journal"),
        BINDINGS(".binds", "bindings");

        private final String suffix;
        private final String key;

        DirectoryKind(String suffix, String key) {
            this.suffix = suffix;
            this.key = key;
        }
    }

    private DataDirectoryValidator() {
    }

    /**
     * Validates {@code directory} for the given kind and, on failure, publishes a mission-critical
     * spoken warning localized via the gui i18n bundle.
     *
     * @return {@code true} when the directory exists and holds at least one matching file.
     */
    public static boolean validateAndWarn(Path directory, DirectoryKind kind) {
        if (directory == null || !Files.isDirectory(directory)) {
            log.warn("{} directory not found: {}", kind.key, directory);
            warn("speech.warning." + kind.key + "DirectoryMissing");
            return false;
        }
        if (!containsMatchingFile(directory, kind.suffix)) {
            log.warn("No {} files ({}) found in {}", kind.key, kind.suffix, directory);
            warn("speech.warning." + kind.key + "FilesMissing");
            return false;
        }
        return true;
    }

    private static boolean containsMatchingFile(Path directory, String suffix) {
        try (Stream<Path> entries = Files.list(directory)) {
            return entries.anyMatch(p -> p.getFileName().toString().toLowerCase().endsWith(suffix));
        } catch (IOException e) {
            log.warn("Could not list directory {}: {}", directory, e.getMessage());
            return false;
        }
    }

    private static void warn(String key) {
        GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedSpeech(key)));
    }
}