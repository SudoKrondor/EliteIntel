package elite.intel.ai.mouth;

import elite.intel.ai.mouth.edge.EdgeVoices;
import elite.intel.ai.mouth.google.GoogleVoices;
import elite.intel.ai.mouth.kokoro.KokoroVoices;
import elite.intel.ai.mouth.supertonic.SupertonicVoices;
import elite.intel.i18n.Language;

/**
 * The engine that voices VEGA. Exactly one is active, and the choice is a stored setting in its own
 * right ({@code game_session.ttsProvider}) - it is never inferred from the shape of the cloud API key.
 * <p>
 * Two engines run locally and need nothing configured: {@link #KOKORO}, which is the shipped default and the
 * fallback for an unreadable stored value, so a commander with no cloud account still has a voice; and
 * {@link #SUPERTONIC}, the alternative local engine, which is the one local engine that can read Cyrillic and
 * therefore stands in for Kokoro wherever Kokoro cannot speak (see {@link #forLanguage}). {@link #GOOGLE} is
 * the only engine that needs an API key. {@link #EDGE} is Microsoft's online Read Aloud service, which is
 * keyless but not local - it still talks to Microsoft over the network.
 */
public enum TtsProvider {
    KOKORO,
    SUPERTONIC,
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
     * The voice a ship gets from this engine until the commander picks one: the name every cast-specific
     * {@code voiceOrDefault} collapses an unknown name to. Every engine names its voices differently, so a
     * new ship, and every ship after an engine switch, is written with the active engine's default.
     */
    public String defaultVoiceName() {
        return switch (this) {
            case KOKORO -> KokoroVoices.DEFAULT_VOICE.name();
            case SUPERTONIC -> SupertonicVoices.DEFAULT_VOICE.name();
            case EDGE -> EdgeVoices.DEFAULT_VOICE.name();
            case GOOGLE -> GoogleVoices.DEFAULT_VOICE.name();
        };
    }

    /**
     * Whether the engine synthesises on this machine, with no network call and no account.
     */
    public boolean isLocal() {
        return this == KOKORO || this == SUPERTONIC;
    }

    /**
     * Whether this engine can voice the language at all - not "voice it well", but produce sound from it.
     * <p>
     * Only {@link #KOKORO} ever answers no: its phonemizer has no Cyrillic front end, so Russian and
     * Ukrainian text is not spoken with an accent, it is not spoken (see {@link Language#isCyrillicScript()}).
     * {@link #SUPERTONIC} is one multilingual model with Russian and Ukrainian among its languages, and
     * {@link #GOOGLE} and {@link #EDGE} carry every language this app ships.
     */
    public boolean canVoice(Language language) {
        return this != KOKORO || !language.isCyrillicScript();
    }

    /**
     * The engine that will actually speak {@code language}: the selection itself wherever it can voice the
     * language, and {@link #SUPERTONIC} where it cannot.
     * <p>
     * Supertonic is the stand-in rather than a cloud engine because it is what the commander asked for in every
     * respect but the name: local, keyless, nothing leaving the machine. A Cyrillic commander did not choose to
     * be in this position - Kokoro is the shipped default - and until Supertonic existed the stand-in was Edge,
     * which cost them a network round trip they never opted into. Google stays selectable - this only decides
     * what happens to a selection that cannot speak.
     */
    public static TtsProvider forLanguage(TtsProvider selected, Language language) {
        return selected.canVoice(language) ? selected : SUPERTONIC;
    }
}
