package elite.intel.ai.mouth;

import elite.intel.i18n.Language;

/**
 * The engine that voices VEGA. Exactly one is active, and the choice is a stored setting in its own
 * right ({@code game_session.ttsProvider}) - it is never inferred from the shape of the cloud API key.
 * <p>
 * {@link #KOKORO} runs locally and needs nothing configured, which is why it is both the shipped default and
 * the fallback for an unreadable stored value: a commander with no cloud account still has a voice. {@link #GOOGLE}
 * is the only engine that needs an API key. {@link #EDGE} is Microsoft's online Read Aloud service, which is
 * keyless but not local - it still talks to Microsoft over the network.
 */
public enum TtsProvider {
    KOKORO,
    GOOGLE,
    EDGE;

    /**
     * Resolves a stored setting value, falling back to {@link #KOKORO} for anything this build does not
     * recognise (null, blank, or a provider written by a newer version).
     */
    public static TtsProvider fromStored(String stored) {
        if (stored == null || stored.isBlank()) {
            return KOKORO;
        }
        for (TtsProvider provider : values()) {
            if (provider.name().equalsIgnoreCase(stored.trim())) {
                return provider;
            }
        }
        return KOKORO;
    }

    /**
     * Whether the engine synthesises on this machine, with no network call and no account.
     */
    public boolean isLocal() {
        return this == KOKORO;
    }

    /**
     * Whether this engine can voice the language at all - not "voice it well", but produce sound from it.
     * <p>
     * Only {@link #KOKORO} ever answers no: its phonemizer has no Cyrillic front end, so Russian and
     * Ukrainian text is not spoken with an accent, it is not spoken (see {@link Language#isCyrillicScript()}).
     * {@link #GOOGLE} and {@link #EDGE} carry every language this app ships.
     */
    public boolean canVoice(Language language) {
        return this != KOKORO || !language.isCyrillicScript();
    }

    /**
     * The engine that will actually speak {@code language}: the selection itself wherever it can voice the
     * language, and {@link #EDGE} where it cannot.
     * <p>
     * Edge is the stand-in rather than Google because it is keyless: a commander whose only choice is a paid
     * account has no voice at all until they open one, and a Cyrillic commander did not choose to be in this
     * position. Google stays selectable - this only decides what happens to a selection that cannot speak.
     */
    public static TtsProvider forLanguage(TtsProvider selected, Language language) {
        return selected.canVoice(language) ? selected : EDGE;
    }
}
