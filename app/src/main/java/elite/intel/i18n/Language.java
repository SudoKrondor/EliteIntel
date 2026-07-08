package elite.intel.i18n;

public enum Language {
    EN("English"),
    RU("Russian"),
    UK("Ukrainian"),
    DE("German"),
    FR("French"),
    ES("Spanish"),
    PT("Portuguese"),
    IT("Italian");

    private final String displayName;

    Language(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Full English display name, e.g. {@code "Russian"}, {@code "German"}. Injected into LLM prompts.
     */
    public String displayName() {
        return displayName;
    }
}
