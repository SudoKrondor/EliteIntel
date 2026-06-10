package elite.intel.ai.mouth;

/**
 * If you implement another TTS, map these voices in your class to the
 * voices available in your TTS provider.
 */
public enum GoogleVoices {

    MARY("Mary", 1.3, false, "American female"),   // en-US-Chirp3-HD-Zephyr
    ANNA("Anna", 1.1, false, "British female"),    // en-GB-Chirp-HD-F
    EMMA("Emma", 1.2, false, "American female"),   // en-US-Chirp3-HD-Despina
    JAKE("Jake", 1.2, false, "American female"),   // en-US-Chirp3-HD-Iapetus
    JAMES("James", 1.2, true, "Australian male"),   // en-AU-Chirp3-HD-Algieba
    JENNIFER("Jennifer", 1.2, false, "American female"), // en-US-Chirp3-HD-Sulafat
    JOSEPH("Joseph", 1.1, true, "American male"),     // en-US-Chirp3-HD-Sadachbia
    MICHAEL("Michael", 1.2, true, "American male"),    // en-US-Chirp3-HD-Charon
    OLIVIA("Olivia", 1.2, false, "British female"),    // en-GB-Chirp3-HD-Aoede
    RACHEL("Rachel", 1.2, false, "American female"),   // en-US-Chirp3-HD-Zephyr
    STEVE("Steve", 1.2, true, "American male"),     // en-US-Chirp3-HD-Algenib
    ;

    private final String name;
    private final double speechRate;
    private final boolean isMale;
    private final String description;

    GoogleVoices(String name, double speechRate, boolean isMale, String description) {
        this.name = name;
        this.speechRate = speechRate;
        this.isMale = isMale;
        this.description = description;
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

    public double getSpeechRate() {
        return speechRate;
    }

    public boolean isMale() {
        return isMale;
    }
}
