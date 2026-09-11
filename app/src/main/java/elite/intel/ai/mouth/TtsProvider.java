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
    SUPERTONIC,
    GOOGLE,
    EDGE;

    /**
     * Resolves a stored setting value, falling back to {@link #SUPERTONIC} for anything this build does not
     * recognise (null, blank, or a provider written by a newer version). A stored value of {@code "KOKORO"}
     * - the engine this one replaced - also falls back here rather than throwing: the migration that renamed
     * the column rewrites the setting itself, but a commander who somehow still has the old value should still
     * get the local engine, not silence.
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
        return this == SUPERTONIC;
    }

    /**
     * Whether this engine can voice the language at all - not "voice it well", but produce sound from it.
     * <p>
     * Every engine answers yes for every language this app ships. Kokoro, the local engine this one replaced,
     * had no Cyrillic front end in its phonemizer and could not voice Russian or Ukrainian at all; Supertonic
     * is a single multilingual model that covers Cyrillic natively (see
     * {@code SupertonicTTS#supertonicLangCode}), so that exception is gone. The method is kept rather than
     * inlined to {@code true} because a future engine could reintroduce a language gap, and every caller here
     * already asks the question the right way.
     */
    public boolean canVoice(Language language) {
        return true;
    }

    /**
     * The engine that will actually speak {@code language}: the selection itself wherever it can voice the
     * language, and {@link #EDGE} where it cannot.
     * <p>
     * Edge is the stand-in rather than Google because it is keyless: a commander whose only choice is a paid
     * account has no voice at all until they open one. Google stays selectable - this only decides what
     * happens to a selection that cannot speak, which no current engine/language pair triggers (see
     * {@link #canVoice}).
     */
    public static TtsProvider forLanguage(TtsProvider selected, Language language) {
        return selected.canVoice(language) ? selected : EDGE;
    }
}
