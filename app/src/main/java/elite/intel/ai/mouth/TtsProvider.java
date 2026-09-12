package elite.intel.ai.mouth;

import elite.intel.i18n.Language;

/**
 * The engine that voices VEGA. Exactly one is active, and the choice is a stored setting in its own
 * right ({@code game_session.ttsProvider}) - it is never inferred from the shape of the cloud API key.
 * <p>
 * {@link #SUPERTONIC} runs locally and needs nothing configured, which is why it is both the shipped default
 * and the fallback for an unreadable stored value: a commander with no cloud account still has a voice.
 * {@link #GOOGLE} is the only engine that needs an API key. {@link #EDGE} is Microsoft's online Read Aloud
 * service, which is keyless but not local - it still talks to Microsoft over the network.
 */
public enum TtsProvider {
    KOKORO,
    SUPERTONIC,
    GOOGLE,
    EDGE;

    /**
     * Resolves a stored setting value, falling back to {@link #SUPERTONIC} for anything this build does not
     * recognise (null, blank, or a provider written by a newer version). A stored value of {@code "KOKORO"}
     * - the engine this one replaced - is still accepted so legacy commanders keep a local voice instead of
     * falling silent.
     */
    public static TtsProvider fromStored(String stored) {
        if (stored == null || stored.isBlank()) {
            return SUPERTONIC;
        }
        for (TtsProvider provider : values()) {
            if (provider.name().equalsIgnoreCase(stored.trim())) {
                return provider;
            }
        }
        return SUPERTONIC;
    }

    /**
     * Whether the engine synthesises on this machine, with no network call and no account.
     */
    public boolean isLocal() {
        return this == SUPERTONIC || this == KOKORO;
    }

    /**
     * Whether this engine can voice the language at all - not "voice it well", but produce sound from it.
     * <p>
    * Kokoro has no Cyrillic front end and Supertonic exposes European Portuguese only; the local engines
    * therefore cover different edges of the language set.
     */
    public boolean canVoice(Language language) {
        return switch (this) {
            case KOKORO -> !language.isCyrillicScript();
            case SUPERTONIC -> language != Language.PTBZ;
            case GOOGLE, EDGE -> true;
        };
    }

    /**
     * The engine that will actually speak {@code language}: the selection itself wherever it can voice the
     * language, and {@link #EDGE} where it cannot.
     * <p>
    * When the other local engine covers the language, it is preferred before falling back to Edge.
     */
    public static TtsProvider forLanguage(TtsProvider selected, Language language) {
        if (selected.canVoice(language)) return selected;
        if (selected == KOKORO && SUPERTONIC.canVoice(language)) return SUPERTONIC;
        if (selected == SUPERTONIC && KOKORO.canVoice(language)) return KOKORO;
        return EDGE;
    }
}
