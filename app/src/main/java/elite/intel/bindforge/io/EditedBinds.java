package elite.intel.bindforge.io;

import java.util.Objects;

/**
 * What one {@link BindingsWriter} edit did to a binds file held in memory: the outcome, and the file's bytes
 * afterwards - the changed bytes when it is {@link BindingSaveResult#SAVED}, the bytes it was given otherwise.
 * <p>
 * The array is held as given, as {@code bindforge.storage.ConfigurationFiles} holds its files, so nobody may change
 * one after handing it over or reading it back.
 */
public record EditedBinds(BindingSaveResult result, byte[] content) {

    public EditedBinds {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(content, "content");
    }

    /** The edit was made. */
    static EditedBinds saved(byte[] content) {
        return new EditedBinds(BindingSaveResult.SAVED, content);
    }

    /** The edit was not made - nothing to change, or refused - and the file is as it was. */
    static EditedBinds unchanged(BindingSaveResult result, byte[] content) {
        if (result == BindingSaveResult.SAVED) throw new IllegalArgumentException("a saved edit changes the file");
        return new EditedBinds(result, content);
    }

    /** Whether the file changed. */
    public boolean changed() {
        return result == BindingSaveResult.SAVED;
    }
}
