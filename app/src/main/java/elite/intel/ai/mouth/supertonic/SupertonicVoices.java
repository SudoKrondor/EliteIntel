package elite.intel.ai.mouth.supertonic;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Speaker IDs for the sherpa-onnx-supertonic-3-tts-int8-2026-05-11 model, which carries 10 of them
 * (one {@code voice.bin} style embedding per speaker index, selected by {@code sid}).
 * <p>
 * The upstream release does not publish names or genders for these ten indices - the model card and the
 * samples page only ever say "Speaker 0" through "Speaker 9". The names used below follow Supertonic's own
 * preset-voice naming scheme (the {@code supertonic} PyPI package ships an originally-4-voice pack later
 * extended with "6 new voice styles: M3, M4, M5, F3, F4, F5" - ten in all, exactly the speakers this fixed
 * sherpa-onnx release bundles). Which index is which sex was <em>measured</em>, not inferred from that history:
 * every speaker was synthesised in English and Russian and its median fundamental taken - sids 0-4 sit at
 * 165-233 Hz, sids 5-9 at 84-140 Hz - so sids 0-4 are the female voices F1-F5 and sids 5-9 the male M1-M5.
 * The gender matters beyond the label: the ship's voice decides how VEGA speaks of itself (see
 * {@code SystemSession.getVoiceGender()}), so a wrong flag here misgenders every line.
 * <p>
 * Unlike Kokoro's 53-voice, per-accent cast, every one of these ten is retained: there is no immersion-breaking
 * outlier to cull, and holding any of them back would remove a tenth of an already small pool. Nothing may
 * assume a contiguous range, a count, or the presence of any particular voice beyond what is declared below:
 * derive from {@link #values()}.
 * <p>
 * The fleet grid renders each voice by its raw enum name; voices are not localized.
 * <p>
 * Source: https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models (sherpa-onnx-supertonic-3-tts-int8-2026-05-11)
 */
public enum SupertonicVoices {

    F1(0, false, "F1", "Female"),
    F2(1, false, "F2", "Female"),
    F3(2, false, "F3", "Female"),
    F4(3, false, "F4", "Female"),
    F5(4, false, "F5", "Female"),
    M1(5, true, "M1", "Male"),
    M2(6, true, "M2", "Male"),
    M3(7, true, "M3", "Male"),
    M4(8, true, "M4", "Male"),
    M5(9, true, "M5", "Male");

    /**
     * The default ship voice, used when a ship has no stored voice or carries a name this engine does not
     * know (see {@link #voiceOrDefault(String)}). It is female because that is what every existing fleet
     * already sounds like; the commander may pick any voice here, male or female.
     */
    public static final SupertonicVoices DEFAULT_VOICE = F1;

    /**
     * A voice for the next radio transmission: any speaker in the model, male or female, because the voice
     * on the other end of a comms link is a stranger and the variety is the point. The commander's own voice
     * is excluded so the two speakers never sound like the same person.
     *
     * @param ownVoiceName the commander's ship voice (an enum name), or {@code null} when none is resolvable
     */
    public static SupertonicVoices randomRadioVoice(String ownVoiceName) {
        return randomRadioVoice(ownVoiceName, Set.of());
    }

    /**
     * The same draw, also skipping voices that belong to a named speaker - a carrier whose traffic control
     * the commander has given a voice. Recognising that voice is the whole point of assigning it, and a
     * passing station answering in it takes that away.
     *
     * @param reserved enum names spoken for elsewhere; an empty pool falls back to ignoring them, because a
     *                 language with one usable voice must still be able to say something
     */
    public static SupertonicVoices randomRadioVoice(String ownVoiceName, Set<String> reserved) {
        SupertonicVoices[] pool = Arrays.stream(values())
                .filter(voice -> !voice.name().equals(ownVoiceName))
                .filter(voice -> reserved == null || !reserved.contains(voice.name()))
                .toArray(SupertonicVoices[]::new);
        if (pool.length > 0) {
            return pool[ThreadLocalRandom.current().nextInt(pool.length)];
        }
        return reserved == null || reserved.isEmpty() ? DEFAULT_VOICE : randomRadioVoice(ownVoiceName);
    }

    /**
     * A voice for a speaker the commander is going to hear again: the same speaker always draws the same
     * voice, for as long as the cast holds it.
     * <p>
     * A pirate is named on every line they transmit ("Dave Knowles" over three quarters of the named chatter
     * in a two-month journal sample), so drawing afresh each time makes one attacker sound like a crowd -
     * indistinguishable, mid-fight, from several attackers. Deriving the voice from the name instead costs
     * nothing to store, survives a restart, and holds across a whole encounter without anyone tracking when
     * an encounter began or ended.
     * <p>
     * The commander's own voice and any voice reserved for a carrier are still skipped, but by walking on to
     * the next speaker in the cast rather than picking again: that keeps every other speaker on the voice they
     * already had when a carrier is given one mid-session. An unnamed speaker - a transmission with nobody
     * attributed to it - is a stranger, and still draws at random.
     *
     * @param speaker who is transmitting, as the game names them; null or blank draws at random
     */
    public static SupertonicVoices radioVoiceFor(String speaker, String ownVoiceName, Set<String> reserved) {
        if (speaker == null || speaker.isBlank()) return randomRadioVoice(ownVoiceName, reserved);
        SupertonicVoices[] cast = values();
        int start = Math.floorMod(speaker.trim().toLowerCase(Locale.ROOT).hashCode(), cast.length);
        for (int step = 0; step < cast.length; step++) {
            SupertonicVoices candidate = cast[(start + step) % cast.length];
            if (candidate.name().equals(ownVoiceName)) continue;
            if (reserved != null && reserved.contains(candidate.name())) continue;
            return candidate;
        }
        // Everyone in the cast is spoken for. Reserving is best-effort, exactly as it is for the random draw.
        return randomRadioVoice(ownVoiceName, reserved);
    }

    /**
     * Resolves a stored ship-voice name to a voice of this engine: the named voice when it is one, otherwise
     * {@link #DEFAULT_VOICE}. The stored voice's gender is preserved - ship voices are male or female by the
     * commander's choice, and that choice also decides how VEGA refers to herself or himself (see
     * {@code SystemSession.getVoiceGender()}). An unknown name (a voice belonging to another engine - a Kokoro
     * name after a switch between the two local engines) or {@code null} collapses to the default. This
     * is the ship-voice seam only; radio picks from {@link #values()} directly and must not route through here.
     */
    public static SupertonicVoices voiceOrDefault(String name) {
        if (name == null) return DEFAULT_VOICE;
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return DEFAULT_VOICE;
        }
    }

    private final int sid;
    private final boolean male;
    private final String displayName;
    private final String description;

    SupertonicVoices(int sid, boolean male, String displayName, String description) {
        this.sid = sid;
        this.male = male;
        this.displayName = displayName;
        this.description = description;
    }

    public int getSid() {
        return sid;
    }

    public boolean isMale() {
        return male;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
