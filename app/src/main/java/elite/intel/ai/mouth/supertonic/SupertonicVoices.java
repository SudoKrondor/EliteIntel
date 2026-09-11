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
 * samples page only ever say "Speaker 0" through "Speaker 9". The names used below (M1-M5, F1-F5) are not
 * invented: they match Supertonic's own preset-voice-style naming scheme (see the {@code supertonic} PyPI
 * package, which ships an originally-4-voice pack later extended with "6 new voice styles: M3, M4, M5, F3,
 * F4, F5" - four before plus six after is exactly the ten speakers this fixed sherpa-onnx release bundles).
 * The mapping from sid to M/F index - sid 0-4 in order to M1-M5, sid 5-9 in order to F1-F5 - is inferred
 * from that history and from the model's speaker ordering, not confirmed by an upstream manifest, but it is
 * the closest available fit and vastly more informative than a bare number in every voice list this app shows.
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

    M1(0, false, "M1", "Female"),
    M2(1, false, "M2", "Female"),
    M3(2, false, "M3", "Female"),
    M4(3, false, "M4", "Female"),
    M5(4, false, "M5", "Female"),
    F1(5, true, "F1", "Male"),
    F2(6, true, "F2", "Male"),
    F3(7, true, "F3", "Male"),
    F4(8, true, "F4", "Male"),
    F5(9, true, "F5", "Male");

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
     * {@code SystemSession.getVoiceGender()}). An unknown name (a voice belonging to another engine, or a
     * Kokoro voice name left over from before this migration) or {@code null} collapses to the default. This
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
