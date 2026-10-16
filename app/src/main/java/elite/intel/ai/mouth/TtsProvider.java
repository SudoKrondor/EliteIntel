package elite.intel.ai.mouth;

import elite.intel.ai.mouth.google.GoogleVoices;
import elite.intel.ai.mouth.kokoro.KokoroVoices;
import elite.intel.ai.mouth.supertonic.SupertonicVoices;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * The engine that voices VEGA. Exactly one is active, and the choice is a stored setting in its own
 * right ({@code game_session.ttsProvider}) - it is never inferred from the shape of the cloud API key.
 * <p>
 * Two engines run locally and need nothing configured: {@link #KOKORO}, which is the shipped default and the
 * fallback for an unreadable stored value, so a commander with no cloud account still has a voice; and
 * {@link #SUPERTONIC}, the alternative local engine. {@link #GOOGLE} is the only engine that needs an API key
 * and the only one that is not local.
 * <p>
 * A stored {@code EDGE} is an engine this build no longer has: Microsoft Edge Read Aloud was removed in
 * v1.1.0026, and migration {@code 11007} moved its commanders to Kokoro. {@link #fromStored} reads anything
 * it does not recognise as Kokoro too, so a row the migration missed still has a voice.
 */
public enum TtsProvider {
    KOKORO,
    SUPERTONIC,
    GOOGLE;

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
            case GOOGLE -> GoogleVoices.DEFAULT_VOICE.name();
        };
    }

    /**
     * A stored voice name resolved against this engine's cast: the name itself when the engine carries it,
     * otherwise {@link #defaultVoiceName()}. A voice belonging to another engine - the usual case after an
     * engine switch - collapses to the default rather than throwing.
     */
    public String voiceOrDefault(String voiceName) {
        return switch (this) {
            case KOKORO -> KokoroVoices.voiceOrDefault(voiceName).name();
            case SUPERTONIC -> SupertonicVoices.voiceOrDefault(voiceName).name();
            case GOOGLE -> GoogleVoices.voiceOrDefault(voiceName).name();
        };
    }

    /**
     * The gender of a stored ship voice as this engine will speak it - the gender of
     * {@link #voiceOrDefault(String)}, so a name this engine does not know reads as its (female) default.
     */
    public VoiceGender voiceGender(String voiceName) {
        return switch (this) {
            case KOKORO -> VoiceGender.of(KokoroVoices.voiceOrDefault(voiceName).isMale());
            case SUPERTONIC -> VoiceGender.of(SupertonicVoices.voiceOrDefault(voiceName).isMale());
            case GOOGLE -> VoiceGender.of(GoogleVoices.voiceOrDefault(voiceName).isMale());
        };
    }

    /**
     * Every voice this engine carries, by enum name, in cast order.
     */
    public Stream<String> voiceRoster() {
        return switch (this) {
            case KOKORO -> Arrays.stream(KokoroVoices.values()).map(Enum::name);
            case SUPERTONIC -> Arrays.stream(SupertonicVoices.values()).map(Enum::name);
            case GOOGLE -> Arrays.stream(GoogleVoices.values()).map(Enum::name);
        };
    }

    /**
     * The voices this engine may speak a radio transmission in, by enum name, in cast order: the
     * {@link #voiceRoster()} less any ship-only voice. This is what a carrier's traffic control is offered.
     */
    public Stream<String> radioVoiceRoster() {
        return switch (this) {
            case KOKORO -> Arrays.stream(KokoroVoices.radioCast()).map(Enum::name);
            case SUPERTONIC, GOOGLE -> voiceRoster();
        };
    }

    /**
     * Whether the engine synthesises on this machine, with no network call and no account.
     */
    public boolean isLocal() {
        return this == KOKORO || this == SUPERTONIC;
    }
}
