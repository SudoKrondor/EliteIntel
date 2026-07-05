package elite.intel.ai.mouth.google;

/**
 * If you implement another TTS, map these voices in your class to the
 * voices available in your TTS provider.
 * <p>
 * The display name is fixed (Mary, Anna, ...), but the actual synthesized voice is localized: for a
 * non-English language the voice's {@link #getCharacter() Chirp3-HD character} is rendered in that language
 * (see {@code GoogleVoiceProvider}).
 */
public enum GoogleVoices {

    MARY("Mary", 1.3, false, "American female", "Zephyr"),          // en-US-Chirp3-HD-Zephyr
    ANNA("Anna", 1.1, false, "British female", "Kore"),            // en-GB-Chirp-HD-F in English; Chirp3-HD "Kore" elsewhere
    EMMA("Emma", 1.2, false, "American female", "Despina"),        // en-US-Chirp3-HD-Despina
    JAKE("Jake", 1.2, true, "American male", "Iapetus"),           // en-US-Chirp3-HD-Iapetus (a male voice)
    JAMES("James", 1.2, true, "Australian male", "Algieba"),        // en-AU-Chirp3-HD-Algieba
    JENNIFER("Jennifer", 1.2, false, "American female", "Sulafat"), // en-US-Chirp3-HD-Sulafat
    JOSEPH("Joseph", 1.1, true, "American male", "Sadachbia"),        // en-US-Chirp3-HD-Sadachbia
    MICHAEL("Michael", 1.2, true, "American male", "Charon"),         // en-US-Chirp3-HD-Charon
    OLIVIA("Olivia", 1.2, false, "British female", "Aoede"),         // en-GB-Chirp3-HD-Aoede
    RACHEL("Rachel", 1.2, false, "American female", "Zephyr"),       // en-US-Chirp3-HD-Zephyr
    STEVE("Steve", 1.2, true, "American male", "Algenib"),           // en-US-Chirp3-HD-Algenib
    ;

    private final String name;
    private final double speechRate;
    private final boolean isMale;
    private final String description;
    /** Chirp3-HD voice character (e.g. "Zephyr"), shared across locales; used to synthesize this voice in a non-English language. */
    private final String character;

    GoogleVoices(String name, double speechRate, boolean isMale, String description, String character) {
        this.name = name;
        this.speechRate = speechRate;
        this.isMale = isMale;
        this.description = description;
        this.character = character;
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

    /** The Chirp3-HD character name, used to build a localized voice name for a non-English language. */
    public String getCharacter() {
        return character;
    }

    public double getSpeechRate() {
        return speechRate;
    }

    public boolean isMale() {
        return isMale;
    }
}
