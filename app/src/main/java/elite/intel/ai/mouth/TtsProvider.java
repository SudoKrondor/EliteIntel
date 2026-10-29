package elite.intel.ai.mouth;

import elite.intel.ai.mouth.google.GoogleVoices;
import elite.intel.ai.mouth.supertonic.SupertonicVoices;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * The engine that voices VEGA. Exactly one is active, and the choice is a stored setting in its own
 * right ({@code game_session.ttsProvider}) - it is never inferred from the shape of the cloud API key.
 * <p>
 * {@link #SUPERTONIC} runs locally and needs nothing configured: it is the default and the fallback for an
 * unreadable stored value, so a commander with no cloud account still has a voice. {@link #GOOGLE} is the only
 * engine that needs an API key and the only one that is not local.
 * <p>
 * A stored {@code EDGE} or {@code KOKORO} is an engine this build no longer has: Microsoft Edge Read Aloud was
 * removed in v1.1.0026, and Kokoro in V1.2 (migration {@code 12015} moved its commanders to Supertonic, whose
 * cast carries the same voice names). {@link #fromStored} reads anything it does not recognise as Supertonic, so
 * a row the migrations missed still has a voice.
 */
public enum TtsProvider {
    SUPERTONIC,
    GOOGLE;

    /**
     * Resolves a stored setting value, falling back to {@link #SUPERTONIC} for anything this build does not
     * recognise (null, blank, or a provider written by a newer version).
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
     * The voice a ship gets from this engine until the commander picks one: the name every cast-specific
     * {@code voiceOrDefault} collapses an unknown name to. Every engine names its voices differently, so a
     * new ship, and every ship after an engine switch, is written with the active engine's default.
     */
    public String defaultVoiceName() {
        return switch (this) {
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
            case SUPERTONIC -> VoiceGender.of(SupertonicVoices.voiceOrDefault(voiceName).isMale());
            case GOOGLE -> VoiceGender.of(GoogleVoices.voiceOrDefault(voiceName).isMale());
        };
    }

    /**
     * Every voice this engine carries, by enum name, in cast order.
     */
    public Stream<String> voiceRoster() {
        return switch (this) {
            case SUPERTONIC -> Arrays.stream(SupertonicVoices.values()).map(Enum::name);
            case GOOGLE -> Arrays.stream(GoogleVoices.values()).map(Enum::name);
        };
    }

    /**
     * The voices this engine may speak a radio transmission in, by enum name, in cast order. No engine has a
     * ship-only voice, so this is the {@link #voiceRoster()}. This is what a carrier's
     * traffic control is offered.
     */
    public Stream<String> radioVoiceRoster() {
        return voiceRoster();
    }

    /**
     * Whether the engine synthesises on this machine, with no network call and no account.
     */
    public boolean isLocal() {
        return this == SUPERTONIC;
    }
}
