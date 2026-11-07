package elite.intel.bindforge.io;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
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
 *   <li><strong>Whether two names are one file</strong> (Krondor and Alan, 2026-11-07, Storage S13a): ignoring case, on
 *       every platform, because the game reads names the Windows way, under Proton too. Every name match in BindForge
 *       asks {@link #sameFile} or keys its map with {@link #ONE_FILE}, and BindForge never leaves two files whose
 *       names differ only in case (storage.md, <em>How the files are written</em>). Only the name ignores case: the
 *       kinds - {@code .binds}, {@code .buttonMap}, {@code StartPreset.N.start} - are matched exactly, since nobody
 *       renames those (Alan, 2026-11-07).</li>
 * </ul>
 */
public final class FileNames {

    /**
     * The order of a map keyed by file name: two names that are one file are one key. Note that {@code put} on such a
     * map keeps the key already there - to save a new spelling, remove the old one first.
     */
    public static final Comparator<String> ONE_FILE = String.CASE_INSENSITIVE_ORDER;

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
    public static boolean plain(String name) {
        if (name == null || name.isBlank() || name.endsWith(" ") || name.endsWith(".")) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < ' ' || NOT_IN_A_NAME.indexOf(c) >= 0) return false;
        }
        return true;
    }

    /** Whether {@code name} is a binds file's name - {@code <name>.<major>.<minor>.binds}. */
    public static boolean isBindsFile(String name) {
        return BINDS_FILE.matcher(name).matches();
    }

    /** Whether {@code name} is a preset file's name - {@code StartPreset.<N>.start}. */
    public static boolean isPresetFile(String name) {
        return PRESET_FILE.matcher(name).matches();
    }

    /** Whether {@code file} is one that goes to the bindings folder: a binds file or a preset file. */
    public static boolean isBindingsFile(Path file) {
        String name = file.getFileName().toString();
        return isBindsFile(name) || isPresetFile(name);
    }

    /** A binds file's BindName - {@code Custom} for {@code Custom.4.2.binds}. Empty for any other name. */
    public static Optional<String> bindName(String bindsFile) {
        Matcher matcher = BINDS_FILE.matcher(bindsFile);
        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    /** A binds file's version as one number, major before minor - {@code -1} when the name is not a binds file's. */
    public static long version(String bindsFile) {
        Matcher matcher = BINDS_FILE.matcher(bindsFile);
        if (!matcher.matches()) return -1;
        // WHY: both numbers bounded so neither overflows into the other.
        return bounded(matcher.group(2)) * (LARGEST + 1) + bounded(matcher.group(3));
    }

    /** A preset file's N - {@code 4} for {@code StartPreset.4.start}; {@code -1} when the name is not a preset file's. */
    public static long presetGeneration(String presetFile) {
        Matcher matcher = PRESET_FILE.matcher(presetFile);
        return matcher.matches() ? bounded(matcher.group(1)) : -1;
    }

    /** Whether {@code one} and {@code other} name one file - or one BindName: they differ at most in case. */
    public static boolean sameFile(String one, String other) {
        return one.equalsIgnoreCase(other);
    }

    /**
     * {@code files} with one spelling of each file: where several names are one file - only on a file system that heeds
     * case, made outside BindForge - the one spelled as a {@code preferred} name is kept, else the first in plain name
     * order (Alan, 2026-11-07, Storage S13a D3). <strong>Kept under the preferred spelling</strong> when one is one
     * file with it, so a live {@code custom.4.2.binds} pairs with the working copy's {@code Custom.4.2.binds} (D2).
     *
     * @param files     files by name, as found
     * @param preferred the spellings to keep and use - the working copy's names; empty for none
     */
    public static <V> OneSpelling<V> oneSpelling(Map<String, V> files, Collection<String> preferred) {
        Map<String, List<String>> groups = new TreeMap<>(ONE_FILE);
        new TreeMap<>(files).keySet().forEach(name -> groups.computeIfAbsent(name, n -> new ArrayList<>()).add(name));
        Map<String, V> kept = new LinkedHashMap<>();
        List<String> leftOut = new ArrayList<>();
        for (List<String> spellings : groups.values()) {
            Optional<String> wanted = preferred.stream().sorted()
                    .filter(name -> sameFile(name, spellings.getFirst())).findFirst();
            String chosen = wanted.filter(spellings::contains).orElse(spellings.getFirst());
            kept.put(wanted.orElse(chosen), files.get(chosen));
            spellings.stream().filter(name -> !name.equals(chosen)).forEach(leftOut::add);
        }
        return new OneSpelling<>(kept, leftOut);
    }

    /**
     * {@code files} as an unmodifiable map keyed with {@link #ONE_FILE}, in name order, so a lookup finds a file
     * whatever its spelling. Each file keeps the spelling it was given.
     *
     * @throws IllegalArgumentException if two names are one file - refused rather than one silently dropped (Storage
     *                                  S13aR, D2); {@link #oneSpelling} is how a caller chooses between them first
     */
    public static <V> Map<String, V> oneFileEach(Map<String, V> files) {
        Map<String, V> sorted = new TreeMap<>(ONE_FILE);
        files.forEach((name, content) -> {
            if (sorted.containsKey(name)) {
                String other = sorted.keySet().stream().filter(found -> sameFile(found, name)).findFirst()
                        .orElseThrow();
                throw new IllegalArgumentException("two names for one file: " + name + " and " + other);
            }
            sorted.put(name, content);
        });
        return Collections.unmodifiableMap(sorted);
    }

    /**
     * Files with one spelling of each, and the other spellings left out.
     *
     * @param files   by name, one spelling of each file
     * @param leftOut the names left out because another spelling of the same file was kept
     */
    public record OneSpelling<V>(Map<String, V> files, List<String> leftOut) {
        public OneSpelling {
            files = Collections.unmodifiableMap(new LinkedHashMap<>(files));
            leftOut = List.copyOf(leftOut);
        }
    }

    private static long bounded(String digits) {
        return digits.length() > MAX_DIGITS ? LARGEST : Long.parseLong(digits);
    }
}
