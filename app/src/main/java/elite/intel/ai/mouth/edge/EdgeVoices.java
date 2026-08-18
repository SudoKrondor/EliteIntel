package elite.intel.ai.mouth.edge;

/**
 * Maps the existing per-ship voice names to stable Edge Read Aloud voices. Every name here intentionally has a
 * {@code GoogleVoices} twin, so switching a fleet to Edge reinterprets the voices already stored rather than
 * needing a representation of its own. {@code EdgeVoicesTest} pins that correspondence.
 */
public enum EdgeVoices {
    MARY("Mary", false, "en-US-EmmaMultilingualNeural"),
    ANNA("Anna", false, "en-GB-SoniaNeural"),
    EMMA("Emma", false, "en-US-AriaNeural"),
    JAKE("Jake", true, "en-US-GuyNeural"),
    JAMES("James", true, "en-AU-WilliamNeural"),
    JENNIFER("Jennifer", false, "en-US-JennyNeural"),
    JOSEPH("Joseph", true, "en-US-DavisNeural"),
    MICHAEL("Michael", true, "en-US-ChristopherNeural"),
    OLIVIA("Olivia", false, "en-GB-LibbyNeural"),
    RACHEL("Rachel", false, "en-US-AvaMultilingualNeural"),
    STEVE("Steve", true, "en-US-BrianNeural");

    public static final EdgeVoices DEFAULT_FEMALE = MARY;

    private final String displayName;
    private final boolean male;
    private final String defaultShortName;

    EdgeVoices(String displayName, boolean male, String defaultShortName) {
        this.displayName = displayName;
        this.male = male;
        this.defaultShortName = defaultShortName;
    }

    public static EdgeVoices femaleOrDefault(String name) {
        EdgeVoices voice = fromName(name, DEFAULT_FEMALE);
        return voice.male ? DEFAULT_FEMALE : voice;
    }

    /** Maps a legacy display/enum name to Edge's ShortName while preserving provider-native ShortNames. */
    public static String femaleShortNameOrDefault(String name) {
        EdgeVoices mapped = find(name);
        if (mapped != null) {
            return mapped.male ? DEFAULT_FEMALE.defaultShortName : mapped.defaultShortName;
        }
        if (name != null && !name.isBlank() && name.endsWith("Neural")) {
            return name;
        }
        return DEFAULT_FEMALE.defaultShortName;
    }

    static EdgeVoices fromName(String name, EdgeVoices fallback) {
        EdgeVoices voice = find(name);
        return voice == null ? fallback : voice;
    }

    /**
     * Resolves a stored voice name in any form this app has ever written for Edge: the logical enum name, the
     * display name, or a provider-native ShortName. Returns {@code null} when the name is none of those.
     */
    public static EdgeVoices find(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        for (EdgeVoices voice : values()) {
            if (voice.name().equalsIgnoreCase(name) || voice.displayName.equalsIgnoreCase(name)
                    || voice.defaultShortName.equals(name)) {
                return voice;
            }
        }
        return null;
    }

    public String displayName() {
        return displayName;
    }

    public boolean male() {
        return male;
    }

    public String defaultShortName() {
        return defaultShortName;
    }
}
