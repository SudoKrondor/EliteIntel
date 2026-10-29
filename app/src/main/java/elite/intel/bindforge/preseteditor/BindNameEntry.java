package elite.intel.bindforge.preseteditor;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * One BindName a line of {@code StartPreset.4.start} can hold, and the file or files that answer to it.
 * <p>
 * A factory binds file has one file per installation that ships it. A user binds file has one file - a set never
 * holds two files for one BindName (Alan, 2026-10-17: the game upgrades {@code Custom.3.0.binds} to
 * {@code Custom.4.2.binds}); if it ever does, both are kept here rather than one silently dropped.
 *
 * @param bindName the BindName, read from the file's {@code PresetName}, exactly as a line of
 *                 {@code StartPreset.4.start} holds it
 * @param origin   the player's folder, or an installation's
 * @param files    every file answering to the BindName, never empty: a factory one by its full path, the player's by
 *                 its file name alone, since it is in the set APPLY writes, not yet necessarily on disk
 */
public record BindNameEntry(String bindName, BindsFileOrigin origin, List<Path> files) {

    public BindNameEntry {
        Objects.requireNonNull(bindName, "bindName");
        Objects.requireNonNull(origin, "origin");
        files = List.copyOf(files);
        if (files.isEmpty()) {
            throw new IllegalArgumentException("A BindName with no file: " + bindName);
        }
    }
}
