package elite.intel.bindforge.preseteditor;

import java.util.regex.Pattern;

/**
 * The BindName a {@code .binds} file's name implies: the extension removed, then the game's version suffix
 * removed <strong>only when there is one</strong>.
 * <p>
 * {@code DualVirpilDawnTreader.4.2.binds} → {@code DualVirpilDawnTreader}; {@code KeyboardMouseOnly.binds}, a
 * factory binds file with no suffix → {@code KeyboardMouseOnly}; {@code Custom.4.2 (2).binds}, de-duplicated by a
 * download, does not end in a suffix → {@code Custom.4.2 (2)}.
 * <p>
 * This is not where a line of {@code StartPreset.4.start} gets its value - that is the file's own
 * {@code PresetName} (Alan, 2026-10-17). It is what that {@code PresetName} must match for the file to be offered.
 */
public final class BindName {

    static final String BINDS_EXTENSION = ".binds";

    private static final Pattern VERSION_SUFFIX = Pattern.compile("\\.\\d+\\.\\d+$");

    private BindName() {
    }

    /**
     * @param fileName a file name ending {@code .binds}
     */
    public static String fromFileName(String fileName) {
        if (!fileName.endsWith(BINDS_EXTENSION)) {
            throw new IllegalArgumentException("Not a .binds file name: " + fileName);
        }
        String stem = fileName.substring(0, fileName.length() - BINDS_EXTENSION.length());
        return VERSION_SUFFIX.matcher(stem).replaceFirst("");
    }
}
