package elite.intel.bindforge.preseteditor;

/**
 * Why a file with a {@code .binds} name is not offered as a binds file. The screen turns each into a sentence
 * saying what the file looks like ({@code preset-editor.md}, "The BindName list"); none of them is ever offered.
 */
public enum NotOfferedReason {

    /** Plain text, a few BindNames on their own lines - a preset file saved under a {@code .binds} name. */
    PRESET_FILE,

    /** Well-formed XML whose root is not {@code <Root>}: another game's or tool's config, or a truncated binds file. */
    OTHER_XML,

    /** A {@code <Root>} with no binding in it. */
    ROOT_WITHOUT_BINDINGS,

    /** A binds file with no {@code PresetName}, so no line of {@code StartPreset.4.start} can name it. */
    NO_PRESET_NAME,

    /** A binds file whose {@code PresetName} is not the BindName its file name gives, such as {@code Custom.4.2 (2).binds}. */
    NAME_MISMATCH,

    /** Neither XML nor a preset file: not well-formed, cut short, or something else entirely. */
    NOT_XML,

    /** Nothing in it, or only whitespace. */
    EMPTY,

    /** The file could not be read at all. */
    UNREADABLE
}
