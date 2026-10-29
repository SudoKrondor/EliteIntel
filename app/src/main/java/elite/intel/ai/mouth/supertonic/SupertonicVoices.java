package elite.intel.ai.mouth.supertonic;

import elite.intel.ai.mouth.sherpa.RadioVoiceDraw;

import java.util.Set;

/**
 * Speaker IDs for the sherpa-onnx-supertonic-3-tts-int8-2026-05-11 model. Each speaker is one style embedding
 * in {@code voice.bin}, and {@code sid} picks it by position: speaker N is the N-th entry in the file. The
 * upstream release ships 10 speakers. Our {@code voice.bin} replaces it and adds the cast the app shipped
 * before V1.2, so one engine speaks every voice in every language Supertonic supports.
 * <p>
 * <b>Sids 0-9, the Supertonic presets.</b> These are the 10 speakers from the upstream file, unchanged. The
 * release does not publish their names or genders; the model card and samples page only say "Speaker 0" through
 * "Speaker 9". The constant names follow Supertonic's own preset naming (the {@code supertonic} PyPI package
 * ships an originally 4-voice pack, later extended with "6 new voice styles: M3, M4, M5, F3, F4, F5"). Which
 * index is which sex was <em>measured</em>: every speaker was synthesised in English and Russian and its median
 * fundamental taken. Sids 0-4 sit at 165-233 Hz and sids 5-9 at 84-140 Hz, so 0-4 are F1-F5 and 5-9 are M1-M5.
 * "F1" and "M3" are model indices, not something to choose a ship's voice by, so these speakers carry
 * space-flavoured display names of our own.
 * <p>
 * <b>Sids 10 and up, the fitted voices.</b> Each of these is a new Supertonic style, fitted to one voice of
 * the pre-V1.2 cast: it starts from the nearest preset and is tuned until its timbre and pitch match that
 * voice's rendering of a long training text. The constant names are the pre-V1.2 voice names, so a ship voice
 * stored before V1.2 resolves to the same voice here (see {@link #voiceOrDefault(String)}). Only the
 * timbre carries over, not the accent or language. Every voice speaks every language with Supertonic's own
 * pronunciation, so no accent is shown for them. The male flags follow the originals'. The two "Alex" voices
 * were told apart by accent, so here they carry display names of their own (Alejandro, Alexandre).
 * <p>
 * The gender matters beyond the label: the ship's voice decides how VEGA speaks of itself (see
 * {@code SystemSession.getVoiceGender()}), so a wrong flag here misgenders every line.
 * <p>
 * <b>Sids must match {@code voice.bin}.</b> The file is packed from this enum by {@code pack_release.py} in the
 * supertonic-voices project, one entry per sid. Sids are contiguous today; a sid this enum skips would still
 * get a filler entry, a copy of F1, so the sids after it do not shift. Adding, removing or renumbering a voice
 * here means repacking {@code voice.bin}. A stale file plays the wrong voice for every sid past the change.
 * <p>
 * The constant name is the stored identity of a ship voice and must never change; the display name is what the
 * commander sees when picking a voice. Nothing may assume a contiguous range, a count, or the presence of any
 * particular voice beyond what is declared below: derive from {@link #values()}.
 * <p>
 * The fleet grid renders each voice as its display name and its gender in the app language. The names are
 * not localized; the description is English and never shown.
 * <p>
 * Licences: the Supertonic model and every style in {@code voice.bin}, fitted ones included, fall under
 * Supertonic's OpenRAIL-M licence and its use restrictions. The voices used as fitting targets are Kokoro-82M's
 * (Apache-2.0); none of that model ships with the app.
 * <p>
 * Sources: model https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models
 * (sherpa-onnx-supertonic-3-tts-int8-2026-05-11); fitted voices: the supertonic-voices project.
 */
public enum SupertonicVoices {

    // Supertonic presets, sids 0-9
    F1(0, false, "Astra", "Female"),
    F2(1, false, "Lyra", "Female"),
    F3(2, false, "Vesper", "Female"),
    F4(3, false, "Nyx", "Female"),
    F5(4, false, "Solara", "Female"),
    M1(5, true, "Orion", "Male"),
    M2(6, true, "Atlas", "Male"),
    M3(7, true, "Rook", "Male"),
    M4(8, true, "Cassian", "Male"),
    M5(9, true, "Draven", "Male"),

    // Fitted voices, sids 10 and up
    ALLOY(10, false, "Alloy", "Female"),
    AOEDE(11, false, "Aoede", "Female"),
    BELLA(12, false, "Bella", "Female"),
    HEART(13, false, "Heart", "Female"),
    JESSICA(14, false, "Jessica", "Female"),
    KORE(15, false, "Kore", "Female"),
    NICOLE(16, false, "Nicole", "Female"),
    NOVA(17, false, "Nova", "Female"),
    RIVER(18, false, "River", "Female"),
    SARAH(19, false, "Sarah", "Female"),
    SKY(20, false, "Sky", "Female"),
    ADAM(21, true, "Adam", "Male"),
    ECHO(22, true, "Echo", "Male"),
    ERIC(23, true, "Eric", "Male"),
    FENRIR(24, true, "Fenrir", "Male"),
    LIAM(25, true, "Liam", "Male"),
    MICHAEL(26, true, "Michael", "Male"),
    ONYX(27, true, "Onyx", "Male"),
    PUCK(28, true, "Puck", "Male"),
    ALICE(29, false, "Alice", "Female"),
    EMMA(30, false, "Emma", "Female"),
    ISABELLA(31, false, "Isabella", "Female"),
    LILY(32, false, "Lily", "Female"),
    DANIEL(33, true, "Daniel", "Male"),
    FABLE(34, true, "Fable", "Male"),
    GEORGE(35, true, "George", "Male"),
    LEWIS(36, true, "Lewis", "Male"),
    ES_ALEX(37, true, "Alejandro", "Male"),
    FR_SIWIS(38, false, "Siwis", "Female"),
    HI_ALPHA(39, false, "Alpha", "Female"),
    HI_BETA(40, false, "Beta", "Female"),
    HI_OMEGA(41, true, "Omega", "Male"),
    HI_PSI(42, true, "Psi", "Male"),
    IT_NICOLA(43, true, "Nicola", "Male"),
    PT_ALEX(44, true, "Alexandre", "Male");

    /**
     * The default ship voice, used when a ship has no stored voice or carries a name this engine does not
     * know (see {@link #voiceOrDefault(String)}). It is female because that is what every existing fleet
     * already sounds like; the commander may pick any voice here, male or female.
     */
    public static final SupertonicVoices DEFAULT_VOICE = FR_SIWIS;

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
     * {@code SystemSession.getVoiceGender()}). An unknown name (a voice belonging to another engine, or one
     * the fitted cast never got) or {@code null} collapses to the default. This
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
