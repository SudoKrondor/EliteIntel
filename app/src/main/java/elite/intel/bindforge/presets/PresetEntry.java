package elite.intel.bindforge.presets;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * One name a {@code StartPreset} line can hold, and the file or files that answer to it.
 * <p>
 * A factory preset has one file per installation that ships it. A user preset has one file - a live folder never
 * holds two files for one name (Alan, 2026-10-17: the game upgrades {@code Custom.3.0.binds} to
 * {@code Custom.4.2.binds}); if it ever does, both are kept here rather than one silently dropped.
 *
 * @param name   the {@code PresetName}, exactly as a line of {@code StartPreset} holds it
 * @param origin the player's folder, or an installation's
 * @param files  every file answering to the name, never empty
 */
public record PresetEntry(String name, PresetOrigin origin, List<Path> files) {

    public PresetEntry {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(origin, "origin");
        files = List.copyOf(files);
        if (files.isEmpty()) {
            throw new IllegalArgumentException("A preset with no file: " + name);
        }
    }
}
