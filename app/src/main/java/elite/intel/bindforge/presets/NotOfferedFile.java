package elite.intel.bindforge.presets;

import java.nio.file.Path;
import java.util.Objects;

/**
 * A file with a {@code .binds} name that is not offered as a preset.
 *
 * @param file   the file
 * @param origin whose folder it was found in
 * @param reason why it is not offered
 * @param detail what it holds instead, for the sentence: the root element for {@code OTHER_XML}, the names for a
 *               preset list, the {@code PresetName} for a mismatch, the parser's or reader's message otherwise;
 *               empty when there is nothing to add
 */
public record NotOfferedFile(Path file, PresetOrigin origin, NotOfferedReason reason, String detail) {

    public NotOfferedFile {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(detail, "detail");
    }
}
