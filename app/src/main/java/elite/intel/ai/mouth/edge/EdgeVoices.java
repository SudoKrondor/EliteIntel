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

    /**
     * The default ship voice, used when a ship has no stored voice or carries a name Edge does not know. It is
     * female because that is what every existing fleet already sounds like; the commander may pick any voice
     * here, male or female.
     */
    public static final EdgeVoices DEFAULT_VOICE = MARY;

    private final String displayName;
    private final boolean male;
    private final String defaultShortName;

    EdgeVoices(String displayName, boolean male, String defaultShortName) {
        this.displayName = displayName;
        this.male = male;
        this.defaultShortName = defaultShortName;
    }

    /**
     * Resolves a stored ship-voice name to an Edge voice, keeping its gender: the commander picks a male or a
     * female voice, and that choice also decides how the companion refers to herself or himself (see
     * {@code SystemSession.getVoiceGender()}). An unrecognised name takes {@link #DEFAULT_VOICE}.
     */
    public static EdgeVoices voiceOrDefault(String name) {
        return fromName(name, DEFAULT_VOICE);
    }

    /**
     * Maps a display/enum name to Edge's ShortName while preserving provider-native ShortNames.
     */
    public static String shortNameOrDefault(String name) {
        EdgeVoices mapped = find(name);
        if (mapped != null) {
            return mapped.defaultShortName;
        }
        if (name != null && !name.isBlank() && name.endsWith("Neural")) {
            return name;
        }
        return DEFAULT_VOICE.defaultShortName;
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
