package elite.intel.i18n;

public enum Language {
    EN("English"),
    DE("German"),
    FR("French"),
    ES("Spanish"),
    PT("Portuguese"),
    PTBZ("Brazilian Portuguese"),
    IT("Italian");

    private final String displayName;

    Language(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Full English display name, e.g. {@code "German"}. Injected into LLM prompts.
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Whether Frontier ships a game client in this language. Of the languages we speak, only English, German,
     * Spanish, French and Brazilian Portuguese are officially localized.
     * <p>
     * This decides which name we speak back for in-game nouns such as materials. A commander running
     * one of these clients reads the localized string on screen, so we must say the same string or they
     * cannot find the item. Everyone else - Italian, European Portuguese - is playing an English client
     * whatever language they speak to us in, so we name the item in English and they can match it to their HUD.
     * <p>
     * Note this governs output only. Input is always matched in the commander's own language: the
     * {@code name_*} columns and {@code material_aliases} cover every language we ship.
     */
    public boolean isGameLocalized() {
        return this == EN || this == DE || this == ES || this == FR || this == PTBZ;
    }
}
