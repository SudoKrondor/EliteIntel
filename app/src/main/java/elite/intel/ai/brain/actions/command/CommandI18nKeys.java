package elite.intel.ai.brain.actions.command;

/** Single owner of the i18n key format for commands: "command.<id>.<field>". */
public final class CommandI18nKeys {

    private static final String PREFIX = "command.";
    private static final String NAME_SUFFIX = ".name";
    private static final String DESCRIPTION_SUFFIX = ".description";

    private CommandI18nKeys() {}

    public static String nameKey(String id) {
        return PREFIX + id + NAME_SUFFIX;
    }

    public static String descriptionKey(String id) {
        return PREFIX + id + DESCRIPTION_SUFFIX;
    }
}
