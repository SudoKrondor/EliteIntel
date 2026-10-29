package elite.intel.bindforge.storage;

import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The file-name rules of the game input configuration files, in one place.
 * <ul>
 *   <li><strong>"A file name, not a path"</strong> (Krondor's review, 2026-10-27): a name BindForge writes into a game
 *       folder or the working copy - from a backup's zip, or a set of files - must not lead out of that folder, and
 *       must be one Windows accepts. Before, the working copy and RESTORE each had a check of their own, and they
 *       differed.</li>
 *   <li><strong>Which names are binds files and preset files</strong>, and the BindName, version and generation a name
 *       carries (review, 2026-10-29: the patterns were in {@code Backups}, {@code BindsEdits} and {@code PresetEdits},
 *       each a little different).</li>
 * </ul>
 */
final class FileNames {

    /** Characters Windows refuses in a name - the path separators among them. */
    private static final String NOT_IN_A_NAME = "<>:\"|?*\\/";

    /**
     * {@code <name>.<major>.<minor>.binds}, the BindName and both numbers captured. A {@code .binds.backup} copy or an
     * unversioned name is not one.
     */
    private static final Pattern BINDS_FILE = Pattern.compile("(.+)\\.(\\d+)\\.(\\d+)\\.binds");
    /** {@code StartPreset.<N>.start}, N captured; a leftover pre-Odyssey {@code StartPreset.start} is not one. */
    private static final Pattern PRESET_FILE = Pattern.compile("StartPreset\\.(\\d+)\\.start");

    /** The most digits a version number is read with; a longer one counts as the largest. */
    private static final int MAX_DIGITS = 6;
    private static final long LARGEST = 999_999;

    private FileNames() {
    }

    /**
     * Whether {@code name} is a plain file name. Refused: blank; a path - a separator, a drive's colon, {@code .} or
     * {@code ..}; a character below a space; one Windows refuses; and a space or dot at the end, which Windows drops,
     * so the file written would not be the one named.
     */
    static boolean plain(String name) {
        if (name == null || name.isBlank() || name.endsWith(" ") || name.endsWith(".")) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < ' ' || NOT_IN_A_NAME.indexOf(c) >= 0) return false;
        }
        return true;
    }

    /** Whether {@code name} is a binds file's name - {@code <name>.<major>.<minor>.binds}. */
    static boolean isBindsFile(String name) {
        return BINDS_FILE.matcher(name).matches();
    }

    /** Whether {@code name} is a preset file's name - {@code StartPreset.<N>.start}. */
    static boolean isPresetFile(String name) {
        return PRESET_FILE.matcher(name).matches();
    }

    /** Whether {@code file} is one that goes to the bindings folder: a binds file or a preset file. */
    static boolean isBindingsFile(Path file) {
        String name = file.getFileName().toString();
        return isBindsFile(name) || isPresetFile(name);
    }

    /** A binds file's BindName - {@code Custom} for {@code Custom.4.2.binds}. Empty for any other name. */
    static Optional<String> bindName(String bindsFile) {
        Matcher matcher = BINDS_FILE.matcher(bindsFile);
        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    /** A binds file's version as one number, major before minor - {@code -1} when the name is not a binds file's. */
    static long version(String bindsFile) {
        Matcher matcher = BINDS_FILE.matcher(bindsFile);
        if (!matcher.matches()) return -1;
        // WHY: both numbers bounded so neither overflows into the other.
        return bounded(matcher.group(2)) * (LARGEST + 1) + bounded(matcher.group(3));
    }

    /** A preset file's N - {@code 4} for {@code StartPreset.4.start}; {@code -1} when the name is not a preset file's. */
    static long presetGeneration(String presetFile) {
        Matcher matcher = PRESET_FILE.matcher(presetFile);
        return matcher.matches() ? bounded(matcher.group(1)) : -1;
    }

    private static long bounded(String digits) {
        return digits.length() > MAX_DIGITS ? LARGEST : Long.parseLong(digits);
    }
}
