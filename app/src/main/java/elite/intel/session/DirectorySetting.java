package elite.intel.session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reading and writing the two folder settings - the game's journal folder and its key-bindings folder -
 * without letting a bad value take the application down with it.
 * <p>
 * <b>Why this exists.</b> A commander arrived with this stored as their journal folder:
 * <pre>C:\...\Elite Dangerous\"C:\...\Elite Dangerous\Journal.2026-08-30T233705.01.log"</pre>
 * which is what {@code JFileChooser} hands back when a quoted path is pasted into its file-name box: it
 * appends the text to the folder being shown, quotes and all. Stored unchecked, every later read of it
 * threw {@link InvalidPathException} - killing the pre-scan thread, and then the Swing thread building the
 * settings panel, so the window never appeared. The one screen that could have corrected the setting was
 * the screen that could not open, and the only way out was deleting the database.
 * <p>
 * So reads never throw, and writes never store something a read cannot survive.
 */
public final class DirectorySetting {

    private static final Logger log = LogManager.getLogger(DirectorySetting.class);

    /**
     * A run of text in double quotes, which is how a pasted path arrives inside a chooser's answer.
     */
    private static final Pattern QUOTED = Pattern.compile("\"([^\"]+)\"");

    private DirectorySetting() {
    }

    /**
     * The folder a stored setting names, or {@code fallback} when it names nothing usable.
     * <p>
     * A path that merely does not exist right now is still returned: a folder on a drive that is not
     * mounted yet is the commander's setting and will be theirs again in a minute, and
     * {@code DataDirectoryValidator} already tells them when a configured folder is missing. Only a value
     * that cannot be turned into a path at all is replaced.
     */
    public static Path resolve(String stored, Path fallback) {
        if (stored == null || stored.isBlank()) {
            return fallback;
        }
        Path parsed = parse(stored);
        if (parsed != null) {
            return parsed;
        }
        log.warn("Stored folder setting is not a usable path, falling back to {}: {}", fallback, stored);
        return fallback;
    }

    /**
     * The folder to store for a value the commander just chose, or empty when nothing usable can be made
     * of it - in which case the caller should keep the previous setting and say so rather than save this.
     * <p>
     * Three shapes are accepted, in order of how likely they are to be what was meant:
     * <ol>
     *   <li>the value itself, when it is a folder;</li>
     *   <li>a path quoted inside it, which is the pasted-into-the-chooser case above;</li>
     *   <li>the value with its quotes stripped out.</li>
     * </ol>
     * A candidate naming a file rather than a folder resolves to the folder holding it, because choosing
     * a journal file when asked for the journal folder is an easy mistake with an obvious intention.
     */
    public static Optional<String> sanitize(String chosen) {
        if (chosen == null || chosen.isBlank()) {
            return Optional.empty();
        }
        for (String candidate : repairCandidates(chosen.trim())) {
            Optional<String> folder = asExistingFolder(candidate);
            if (folder.isPresent()) {
                return folder;
            }
        }
        log.warn("Rejected an unusable folder choice: {}", chosen);
        return Optional.empty();
    }

    private static Optional<String> asExistingFolder(String candidate) {
        Path path = parse(candidate);
        if (path == null) {
            return Optional.empty();
        }
        if (Files.isDirectory(path)) {
            return Optional.of(path.toString());
        }
        if (Files.isRegularFile(path)) {
            Path parent = path.getParent();
            return parent == null ? Optional.empty() : Optional.of(parent.toString());
        }
        return Optional.empty();
    }

    private static List<String> repairCandidates(String chosen) {
        List<String> candidates = new ArrayList<>(3);
        candidates.add(chosen);
        Matcher quoted = QUOTED.matcher(chosen);
        if (quoted.find()) {
            candidates.add(quoted.group(1));
        }
        if (chosen.indexOf('"') >= 0) {
            candidates.add(chosen.replace("\"", ""));
        }
        return candidates;
    }

    /**
     * @return the path, or null when the text cannot be one on this platform
     */
    private static Path parse(String text) {
        try {
            return Paths.get(text).normalize();
        } catch (InvalidPathException e) {
            return null;
        }
    }
}
