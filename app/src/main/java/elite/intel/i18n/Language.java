package elite.intel.i18n;

public enum Language {
    EN("English"),
    RU("Russian"),
    UK("Ukrainian"),
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
     * Full English display name, e.g. {@code "Russian"}, {@code "German"}. Injected into LLM prompts.
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Whether this language is written in Cyrillic script. This is the dividing line for the local Kokoro
     * TTS: its phonemizer has no Cyrillic front end, so Cyrillic text cannot be voiced at all. Every other
     * language we ship is Latin-script and Kokoro will speak it — with an accent when it has no native voice
     * for it, which is acceptable. So Cyrillic is what forces English output, not "language without a Kokoro
     * voice".
     */
    public boolean isCyrillicScript() {
        return this == RU || this == UK;
    }

    /**
     * Whether Frontier ships a game client in this language. Only English, German, Spanish, French,
     * Russian and Brazilian Portuguese are officially localized.
     * <p>
     * This decides which name we speak back for in-game nouns such as materials. A commander running
     * one of these clients reads the localized string on screen, so we must say the same string or they
     * cannot find the item. Everyone else — Ukrainian, Italian, European Portuguese — is playing an
     * English client whatever language they speak to us in, so we name the item in English and they can
     * match it to their HUD.
     * <p>
     * Note this governs output only. Input is always matched in the commander's own language: the
     * {@code name_*} columns and {@code material_aliases} cover all nine, so a Ukrainian speaker asks
     * for "Залізо" and hears "Iron".
     */
    public boolean isGameLocalized() {
        return this == EN || this == DE || this == ES || this == FR || this == RU || this == PTBZ;
    }
}
