package elite.intel.ai.mouth.google;

/**
 * If you implement another TTS, map these voices in your class to the
 * voices available in your TTS provider.
 * <p>
 * The display name is fixed (Mary, Anna, ...), but the character voices are localized: for a non-English
 * language their {@link #getCharacter() Chirp3-HD character} is rendered in that language. Explicit English
 * provider voices instead use the locale's safe female fallback (see {@code GoogleVoiceProvider}).
 */
public enum GoogleVoices {

    MARY("Mary", false, "American female", "Zephyr"),          // en-US-Chirp3-HD-Zephyr
    ANNA("Anna", false, "British female", "Kore"),            // en-GB-Chirp-HD-F in English; Chirp3-HD "Kore" elsewhere
    EMMA("Emma", false, "American female", "Despina"),        // en-US-Chirp3-HD-Despina
    JAKE("Jake", true, "American male", "Iapetus"),           // en-US-Chirp3-HD-Iapetus (a male voice)
    JAMES("James", true, "Australian male", "Algieba"),        // en-AU-Chirp3-HD-Algieba
    JENNIFER("Jennifer", false, "American female", "Sulafat"), // en-US-Chirp3-HD-Sulafat
    JOSEPH("Joseph", true, "American male", "Sadachbia"),        // en-US-Chirp3-HD-Sadachbia
    MICHAEL("Michael", true, "American male", "Charon"),         // en-US-Chirp3-HD-Charon
    OLIVIA("Olivia", false, "British female", "Aoede"),         // en-GB-Chirp3-HD-Aoede
    RACHEL("Rachel", false, "American female", "Zephyr"),       // en-US-Chirp3-HD-Zephyr
    STEVE("Steve", true, "American male", "Algenib"),           // en-US-Chirp3-HD-Algenib
    WAVENET_F("WaveNet F", false, "British female, WaveNet"),     // en-GB-Wavenet-F
    WAVENET_N("WaveNet N", false, "British female, WaveNet"),     // en-GB-Wavenet-N
    ;

    /**
     * Default female voice. Ship voices are female-only (see {@link #femaleOrDefault(String)}); a ship with
     * no stored voice, an unknown voice, or a legacy male voice resolves to this.
     */
    public static final GoogleVoices DEFAULT_FEMALE = MARY;

    /**
     * Resolves a stored ship-voice name to a female voice: the named voice when it is a valid female voice,
     * otherwise {@link #DEFAULT_FEMALE}. Ship voices are female-only, so a male name (a legacy selection from
     * before that constraint), a name valid only for the other TTS provider, or a {@code null} name collapses
     * to the default female.
     */
    public static GoogleVoices femaleOrDefault(String name) {
        if (name == null) return DEFAULT_FEMALE;
        try {
            GoogleVoices v = valueOf(name);
            return v.isMale() ? DEFAULT_FEMALE : v;
        } catch (IllegalArgumentException e) {
            return DEFAULT_FEMALE;
        }
    }

    private final String name;
    private final boolean isMale;
    private final String description;
    /** Chirp3-HD voice character (e.g. "Zephyr"), shared across locales; null for an explicit English-only voice. */
    private final String character;
    private final boolean englishOnly;

    GoogleVoices(String name, boolean isMale, String description, String character) {
        this.name = name;
        this.isMale = isMale;
        this.description = description;
        this.character = character;
        this.englishOnly = false;
    }

    GoogleVoices(String name, boolean isMale, String description) {
        this.name = name;
        this.isMale = isMale;
        this.description = description;
        this.character = null;
        this.englishOnly = true;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /** The Chirp3-HD character name, or null for an explicit English-only provider voice. */
    public String getCharacter() {
        return character;
    }

    /** Explicit provider voices cannot be localized by replacing their locale prefix. */
    public boolean isEnglishOnly() {
        return englishOnly;
    }

    public boolean isMale() {
        return isMale;
    }
}
