package elite.intel.ai.mouth.edge;

/** One voice returned by Microsoft Edge's Read Aloud voice-list endpoint. */
record EdgeVoice(String name, String shortName, String gender, String locale, String suggestedCodec) {
    EdgeVoice {
        if (shortName == null || shortName.isBlank()) {
            throw new IllegalArgumentException("Edge voice ShortName must not be blank");
        }
        if (gender == null || gender.isBlank()) {
            throw new IllegalArgumentException("Edge voice Gender must not be blank");
        }
        if (locale == null || locale.isBlank()) {
            throw new IllegalArgumentException("Edge voice Locale must not be blank");
        }
    }

    boolean male() {
        return "Male".equalsIgnoreCase(gender);
    }

    String protocolName() {
        return name == null || name.isBlank() ? EdgeSsml.protocolVoiceName(shortName) : name;
    }
}
