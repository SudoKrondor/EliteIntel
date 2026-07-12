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
}
