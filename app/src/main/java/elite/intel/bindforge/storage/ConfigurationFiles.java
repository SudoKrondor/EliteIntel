package elite.intel.bindforge.storage;

import elite.intel.bindforge.io.FileNames;
import elite.intel.bindforge.install.GameInstallation;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * One set of the game input configuration files, as bytes: every {@code .binds} and {@code StartPreset.N.start}, one
 * {@code DeviceMappings.xml} and every {@code .buttonMap} (principles.md, <em>Words</em>). What the working copy holds,
 * and what the write path writes to every live copy.
 * <p>
 * Bytes, never parsed: a file is written back exactly as it was read, byte-order mark and line endings included.
 * The arrays are held as given, so nobody may change one after handing it over or reading it back.
 * <p>
 * <strong>One spelling of each file</strong> (Storage S13a, 2026-11-07): names that differ only in case are one file
 * ({@link FileNames#sameFile}), so a set never holds two of them, and a file is looked up ignoring case. Each is held
 * under the spelling it was given, and written under it.
 */
public final class ConfigurationFiles {

    private final Map<String, byte[]> bindings;
    private final byte[] deviceMappings;
    private final Map<String, byte[]> buttonMaps;

    /**
     * @param bindings       every {@code <name>.<major>.<minor>.binds} and {@code StartPreset.N.start}, by file name
     * @param deviceMappings the one {@code DeviceMappings.xml}, or {@code null} when the set holds none
     * @param buttonMaps     every {@code .buttonMap}, by file name ({@code LVWAP.buttonMap})
     * @throws IllegalArgumentException if a name is a path, or not a file of its kind, or two names are one file
     */
    public ConfigurationFiles(Map<String, byte[]> bindings, byte[] deviceMappings, Map<String, byte[]> buttonMaps) {
        bindings.keySet().forEach(ConfigurationFiles::requireBindingsFileName);
        buttonMaps.keySet().forEach(ConfigurationFiles::requireButtonMapName);
        this.bindings = oneFileEach(bindings);
        this.deviceMappings = deviceMappings;
        this.buttonMaps = oneFileEach(buttonMaps);
    }

    // WHY: sorted, so the files are written and logged in one order every time; keyed ignoring case, so a lookup finds
    // the file whatever its spelling and two spellings of one file are refused rather than one silently dropped.
    private static Map<String, byte[]> oneFileEach(Map<String, byte[]> files) {
        Map<String, byte[]> sorted = new TreeMap<>(FileNames.ONE_FILE);
        files.forEach((name, content) -> {
            if (sorted.containsKey(name)) {
                throw new IllegalArgumentException("two names for one file: " + name + " and "
                        + sorted.keySet().stream().filter(other -> FileNames.sameFile(other, name)).findFirst()
                        .orElseThrow());
            }
            sorted.put(name, content);
        });
        return Collections.unmodifiableMap(sorted);
    }

    /**
     * Every {@code .binds} and {@code StartPreset.N.start}, by file name - they go to the bindings folder. Looked up
     * ignoring case.
     */
    public Map<String, byte[]> bindings() {
        return bindings;
    }

    /** Whether the set holds a {@code DeviceMappings.xml}. */
    public boolean hasDeviceMappings() {
        return deviceMappings != null;
    }

    /**
     * The one {@code DeviceMappings.xml}.
     *
     * @throws IllegalStateException if the set holds none - ask {@link #hasDeviceMappings()} first
     */
    public byte[] deviceMappings() {
        if (deviceMappings == null) throw new IllegalStateException("this set holds no DeviceMappings.xml");
        return deviceMappings;
    }

    /** Every {@code .buttonMap}, by file name, looked up ignoring case. */
    public Map<String, byte[]> buttonMaps() {
        return buttonMaps;
    }

    /**
     * Whether {@code other} holds exactly these files, byte for byte - how the edits on screen are told from the
     * working copy they started from (Storage S4a).
     */
    public boolean sameAs(ConfigurationFiles other) {
        return sameFiles(bindings, other.bindings)
                && Arrays.equals(deviceMappings, other.deviceMappings)
                && sameFiles(buttonMaps, other.buttonMaps);
    }

    private static boolean sameFiles(Map<String, byte[]> one, Map<String, byte[]> other) {
        // WHY: the spellings too - the key sets would call two spellings of one file equal.
        if (!List.copyOf(one.keySet()).equals(List.copyOf(other.keySet()))) return false;
        for (Map.Entry<String, byte[]> file : one.entrySet()) {
            if (!Arrays.equals(file.getValue(), other.get(file.getKey()))) return false;
        }
        return true;
    }

    /** These files with {@code deviceMappings} in place of this set's. */
    public ConfigurationFiles withDeviceMappings(byte[] deviceMappings) {
        return new ConfigurationFiles(bindings, deviceMappings, buttonMaps);
    }

    /**
     * These files with one binds or preset file in place of this set's - added when the set holds none of that name.
     * Held under {@code name}'s spelling, in place of any other spelling of it.
     *
     * @throws IllegalArgumentException if the name is a path, or not a binds or preset file's
     */
    public ConfigurationFiles withBindingsFile(String name, byte[] content) {
        Map<String, byte[]> changed = new TreeMap<>(bindings);
        changed.keySet().removeIf(other -> FileNames.sameFile(other, name));
        changed.put(name, Objects.requireNonNull(content, "content"));
        return new ConfigurationFiles(changed, deviceMappings, buttonMaps);
    }

    /** These files with {@code bindings} - every binds and preset file - in place of this set's. */
    public ConfigurationFiles withBindings(Map<String, byte[]> bindings) {
        return new ConfigurationFiles(bindings, deviceMappings, buttonMaps);
    }

    /** These files with {@code buttonMaps} in place of this set's. */
    public ConfigurationFiles withButtonMaps(Map<String, byte[]> buttonMaps) {
        return new ConfigurationFiles(bindings, deviceMappings, buttonMaps);
    }

    private static void requireBindingsFileName(String name) {
        requirePlainName(name);
        if (!FileNames.isBindingsFile(Path.of(name))) {
            throw new IllegalArgumentException("not a binds or StartPreset file name: " + name);
        }
    }

    private static void requireButtonMapName(String name) {
        requirePlainName(name);
        if (!name.endsWith(GameInstallation.BUTTON_MAP_SUFFIX)) {
            throw new IllegalArgumentException("not a .buttonMap file name: " + name);
        }
    }

    // WHY: a name is resolved against a game folder when it is written, so "..\x", "C:x" or a full path would write
    // outside it - the one check RESTORE's backup reading uses too (Krondor's review, 2026-10-27).
    private static void requirePlainName(String name) {
        if (!FileNames.plain(name)) throw new IllegalArgumentException("a file name, not a path: " + name);
    }
}
