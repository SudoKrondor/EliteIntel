package elite.intel.ai.mouth.supertonic;

import elite.intel.ai.mouth.sherpa.RadioVoiceDraw;

import java.util.Set;

/**
 * Speaker IDs for the sherpa-onnx-supertonic-3-tts-int8-2026-05-11 model, which carries 10 of them
 * (one {@code voice.bin} style embedding per speaker index, selected by {@code sid}).
 * <p>
 * The upstream release does not publish names or genders for these ten indices - the model card and the
 * samples page only ever say "Speaker 0" through "Speaker 9". The constant names follow Supertonic's own
 * preset-voice naming scheme (the {@code supertonic} PyPI package ships an originally-4-voice pack later
 * extended with "6 new voice styles: M3, M4, M5, F3, F4, F5" - ten in all, exactly the speakers this fixed
 * sherpa-onnx release bundles). Which index is which sex was <em>measured</em>, not inferred from that history:
 * every speaker was synthesised in English and Russian and its median fundamental taken - sids 0-4 sit at
 * 165-233 Hz, sids 5-9 at 84-140 Hz - so sids 0-4 are the female voices F1-F5 and sids 5-9 the male M1-M5.
 * The gender matters beyond the label: the ship's voice decides how VEGA speaks of itself (see
 * {@code SystemSession.getVoiceGender()}), so a wrong flag here misgenders every line.
 * <p>
 * The constant name is the stored identity of a ship voice (see {@link #voiceOrDefault(String)}) and must
 * never change; the display name is what the commander sees when picking a voice. "F1" and "M3" are model
 * indices, not something to choose a ship's voice by, so each speaker carries a name in the Commander tab
 * instead - space-flavoured, one per speaker, with nothing to imply an accent or a language the way Kokoro's
 * per-accent cast does. The names are ours, not the model's.
 * <p>
 * Unlike Kokoro's 53-voice, per-accent cast, every one of these ten is retained: there is no immersion-breaking
 * outlier to cull, and holding any of them back would remove a tenth of an already small pool. Nothing may
 * assume a contiguous range, a count, or the presence of any particular voice beyond what is declared below:
 * derive from {@link #values()}.
 * <p>
 * The fleet grid renders each voice by its display name; voices are not localized.
 * <p>
 * Source: https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models (sherpa-onnx-supertonic-3-tts-int8-2026-05-11)
 */
public enum SupertonicVoices {

    F1(0, false, "Astra", "Female"),
    F2(1, false, "Lyra", "Female"),
    F3(2, false, "Vesper", "Female"),
    F4(3, false, "Nyx", "Female"),
    F5(4, false, "Solara", "Female"),
    M1(5, true, "Orion", "Male"),
    M2(6, true, "Atlas", "Male"),
    M3(7, true, "Rook", "Male"),
    M4(8, true, "Cassian", "Male"),
    M5(9, true, "Draven", "Male");

    /**
     * The default ship voice, used when a ship has no stored voice or carries a name this engine does not
     * know (see {@link #voiceOrDefault(String)}). It is female because that is what every existing fleet
     * already sounds like; the commander may pick any voice here, male or female.
     */
    public static final SupertonicVoices DEFAULT_VOICE = F1;

    /**
     * A voice for the next radio transmission: any speaker in the cast other than the commander's own. The
     * rules are {@link RadioVoiceDraw}'s; this only supplies the cast.
     *
     * @param ownVoiceName the commander's ship voice (an enum name), or {@code null} when none is resolvable
     */
    public static SupertonicVoices randomRadioVoice(String ownVoiceName) {
        return randomRadioVoice(ownVoiceName, Set.of());
    }

    /**
     * The same draw, also skipping voices reserved for a named speaker (see {@link RadioVoiceDraw#random}).
     */
    public static SupertonicVoices randomRadioVoice(String ownVoiceName, Set<String> reserved) {
        return RadioVoiceDraw.random(values(), DEFAULT_VOICE, ownVoiceName, reserved);
    }

    /**
     * One voice per named speaker, for as long as the cast holds it (see {@link RadioVoiceDraw#forSpeaker}).
     */
    public static SupertonicVoices radioVoiceFor(String speaker, String ownVoiceName, Set<String> reserved) {
        return RadioVoiceDraw.forSpeaker(values(), DEFAULT_VOICE, speaker, ownVoiceName, reserved);
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
