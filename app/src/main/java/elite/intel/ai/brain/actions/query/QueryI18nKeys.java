package elite.intel.ai.brain.actions.query;

/** Single owner of the i18n key format for queries: "query.<id>.<field>". */
public final class QueryI18nKeys {

    private static final String PREFIX = "query.";
    private static final String NAME_SUFFIX = ".name";
    private static final String DESCRIPTION_SUFFIX = ".description";

    private QueryI18nKeys() {}

    public static String nameKey(String id) {
        return PREFIX + id + NAME_SUFFIX;
    }

    public static String descriptionKey(String id) {
        return PREFIX + id + DESCRIPTION_SUFFIX;
    }
}
