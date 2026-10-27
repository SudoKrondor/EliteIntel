package elite.intel.ai.mouth.supertonic;

import elite.intel.ai.mouth.sherpa.RadioVoiceDraw;

import java.util.Set;

/**
 * Speaker IDs for the sherpa-onnx-supertonic-3-tts-int8-2026-05-11 model. Each speaker is one style embedding
 * in {@code voice.bin}, and {@code sid} picks it by position: speaker N is the N-th entry in the file. The
 * upstream release ships 10 speakers. Our {@code voice.bin} replaces it and adds the cast that used to come
 * from Kokoro, so one engine speaks every voice in every language Supertonic supports.
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
 * <b>Sids 10 and up, voices fitted to Kokoro.</b> Each of these is a new Supertonic style, fitted to one voice of
 * the former Kokoro cast: it starts from the nearest preset and is tuned until its timbre and pitch match
 * Kokoro's rendering of a long training text. The constant names are the {@code KokoroVoices} names, so a ship
 * voice stored under Kokoro resolves to the same voice here (see {@link #voiceOrDefault(String)}). Only the
 * timbre carries over, not the accent or language. Every voice speaks every language with Supertonic's own
 * pronunciation, so a description such as "Spanish female" names the Kokoro voice it came from, not how it
 * speaks. The male flags follow Kokoro's.
 * <p>
 * The gender matters beyond the label: the ship's voice decides how VEGA speaks of itself (see
 * {@code SystemSession.getVoiceGender()}), so a wrong flag here misgenders every line.
 * <p>
 * <b>Sids must match {@code voice.bin}.</b> The file is packed from this enum by {@code pack_release.py} in the
 * supertonic-voices project, one entry per sid. A sid this enum does not use (29, Kokoro's culled Santa) still
 * holds a filler entry, a copy of F1, so the sids after it do not shift. Adding, removing or renumbering a voice
 * here means repacking {@code voice.bin}. A stale file plays the wrong voice for every sid past the change.
 * <p>
 * The constant name is the stored identity of a ship voice and must never change; the display name is what the
 * commander sees when picking a voice. Nothing may assume a contiguous range, a count, or the presence of any
 * particular voice beyond what is declared below: derive from {@link #values()}.
 * <p>
 * The fleet grid renders each voice by its display name; voices are not localized.
 * <p>
 * Licences: the Supertonic model and every style in {@code voice.bin}, fitted ones included, fall under
 * Supertonic's OpenRAIL-M licence and its use restrictions. The Kokoro voices used as fitting targets are
 * Apache-2.0.
 * <p>
 * Sources: model https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models
 * (sherpa-onnx-supertonic-3-tts-int8-2026-05-11); fitted voices: the supertonic-voices project.
 */
public enum SupertonicVoices {

    /// Supertonic Default
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

    /// Custom voices
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

    // --- American Male (am_) ---
    ADAM(21, true, "Adam", "Male"),
    ECHO(22, true, "Echo", "Male"),
    ERIC(23, true, "Eric", "Male"),
    FENRIR(24, true, "Fenrir", "Male"),
    LIAM(25, true, "Liam", "Male"),
    MICHAEL(26, true, "Michael", "Male"),
    ONYX(27, true, "Onyx", "Male"),
    PUCK(28, true, "Puck", "Male"),
    //SANTA_AM(19, true, "Santa", "Male"),

    // --- British Female (bf_) ---
    ALICE(29, false, "Alice", "Female"),
    EMMA(30, false, "Emma", "Female"),
    ISABELLA(31, false, "Isabella", "Female"),
    LILY(32, false, "Lily", "Female"),

    // --- British Male (bm_) ---
    DANIEL(33, true, "Daniel", "Male"),
    FABLE(34, true, "Fable", "Male"),
    GEORGE(35, true, "George", "Male"),
    LEWIS(36, true, "Lewis", "Male"),

    // --- Spanish (ef_ / em_) ---
    ES_DORA(37, false, "Dora", "Female"),
    ES_ALEX(38, true, "Alex", "Male"),

    // --- French (ff_) ---
    FR_SIWIS(39, false, "Siwis", "Female"),

    // --- Hindi (hf_ / hm_) ---
    HI_ALPHA(40, false, "Alpha", "Female"),
    HI_BETA(41, false, "Beta", "Female"),
    HI_OMEGA(42, true, "Omega", "Male"),
    HI_PSI(43, true, "Psi", "Male"),

    // --- Italian (if_ / im_) ---
    IT_SARA(44, false, "Sara", "Female"),
    IT_NICOLA(45, true, "Nicola", "Male"),

    // --- Portuguese (pf_ / pm_) ---
    PT_DORA(46, false, "Dora", "Female"),
    PT_ALEX(47, true, "Alex", "Male");



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
