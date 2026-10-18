package elite.intel.bindforge.presets;

/**
 * Why a file with a {@code .binds} name is not offered as a preset. The screen turns each into a sentence saying
 * what the file looks like ({@code preset-editor.md}, "The preset list"); none of them is ever offered.
 */
public enum NotOfferedReason {

    /** Plain text, a few preset names on their own lines - a {@code StartPreset} saved under a {@code .binds} name. */
    START_PRESET_LIST,

    /** Well-formed XML whose root is not {@code <Root>}: another game's or tool's config, or a truncated binds file. */
    OTHER_XML,

    /** A {@code <Root>} with no binding in it. */
    ROOT_WITHOUT_BINDINGS,

    /** A binds file with no {@code PresetName}, so no line of {@code StartPreset} can name it. */
    NO_PRESET_NAME,

    /** A binds file whose {@code PresetName} is not the name its file name gives, such as {@code Custom.4.2 (2).binds}. */
    NAME_MISMATCH,

    /** Neither XML nor a preset list: not well-formed, cut short, or something else entirely. */
    NOT_XML,

    /** Nothing in it, or only whitespace. */
    EMPTY,

    /** The file could not be read at all. */
    UNREADABLE
}
