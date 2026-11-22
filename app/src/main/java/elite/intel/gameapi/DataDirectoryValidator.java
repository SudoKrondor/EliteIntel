package elite.intel.gameapi;

import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.bindforge.io.BindingsLoader;
import elite.intel.eventbus.GameEventBus;
import elite.intel.util.StringUtls;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Validates that a configured data directory actually exists and contains the files we expect to
 * process - journal logs for {@link DirectoryKind#JOURNAL} or the game's preset file for
 * {@link DirectoryKind#BINDINGS}.
 * <p>
 * The user can point either directory at the wrong place (or empty it out), so this guard is used in
 * two situations:
 * <ol>
 *     <li>when the user picks a directory in the UI, and</li>
 *     <li>when the {@code JournalParser} service starts against the configured directory (the bindings
 *     folder is checked at startup by {@code SetupCheck}).</li>
 * </ol>
 * On failure it raises a {@link MissionCriticalAnnouncementEvent}, which is always vocalized
 * regardless of speech settings, so the commander is told immediately. Warning text is localized via
 * the gui i18n bundle.
 */
public final class DataDirectoryValidator {

    private static final Logger log = LogManager.getLogger(DataDirectoryValidator.class);

    /**
     * The kind of data directory, carrying the file name that marks it and its i18n key fragment.
     */
    public enum DirectoryKind {
        JOURNAL(name -> name.toLowerCase().endsWith(".log"), "journal"),
        /**
         * Marked by the game's preset file, not by a {@code .binds}: the game writes {@code StartPreset.*.start} on its
         * first run, but a {@code .binds} only once the controls are customised, so a fresh install on a stock preset
         * is the right folder with no {@code .binds} in it (Krondor, 2026-11-16).
         */
        BINDINGS(BindingsLoader::isStartPresetFileName, "bindings");

        private final Predicate<String> marksFolder;
        private final String key;

        DirectoryKind(Predicate<String> marksFolder, String key) {
            this.marksFolder = marksFolder;
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
        if (!containsMatchingFile(directory, kind.marksFolder)) {
            log.warn("No {} files found in {}", kind.key, directory);
            warn("speech.warning." + kind.key + "FilesMissing");
            return false;
        }
        return true;
    }

    /**
     * Warns that a folder the commander just chose could not be used at all, so their previous setting
     * still stands. Distinct from {@link #validateAndWarn}: that one reports a real folder holding the
     * wrong things, this one reports a choice that was never a folder.
     */
    public static void warnUnusable(DirectoryKind kind) {
        log.warn("Rejected an unusable {} folder choice", kind.key);
        warn("speech.warning." + kind.key + "DirectoryUnusable");
    }

    private static boolean containsMatchingFile(Path directory, Predicate<String> marksFolder) {
        try (Stream<Path> entries = Files.list(directory)) {
            return entries.anyMatch(p -> marksFolder.test(p.getFileName().toString()));
        } catch (IOException e) {
            log.warn("Could not list directory {}: {}", directory, e.getMessage());
            return false;
        }
    }

    private static void warn(String key) {
        GameEventBus.publish(new MissionCriticalAnnouncementEvent(StringUtls.localizedSpeech(key)));
    }
}