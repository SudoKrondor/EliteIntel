package elite.intel.bindforge.preseteditor;

import java.nio.file.Path;
import java.util.Objects;

/**
 * A file with a {@code .binds} name that is not offered as a binds file.
 *
 * @param file   the file: a factory one by its full path, the player's by its file name alone
 * @param origin whose it is
 * @param reason why it is not offered
 * @param detail what it holds instead, for the sentence: the root element for {@code OTHER_XML}, the names for a
 *               preset file, the {@code PresetName} for a mismatch, the parser's or reader's message otherwise;
 *               empty when there is nothing to add
 */
public record NotOfferedFile(Path file, BindsFileOrigin origin, NotOfferedReason reason, String detail) {

    public NotOfferedFile {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(detail, "detail");
    }
}
