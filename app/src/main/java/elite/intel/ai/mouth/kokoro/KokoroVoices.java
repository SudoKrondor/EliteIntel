package elite.intel.ai.mouth.kokoro;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Speaker IDs for the kokoro-multi-lang-v1_0 model, which carries 53 of them.
 * <p>
 * <b>The cast is curated, and smaller than the model.</b> A voice that breaks immersion - a whisper, a Santa,
 * an accent that has no business on a comms link - is commented out below rather than deleted, so its speaker
 * ID stays visible and is never handed to another voice. Nothing may assume a contiguous range, a count, or
 * the presence of any particular voice: derive from {@link #values()}. A name that has left the cast is still
 * stored in the databases of commanders who were using it, which is what {@link #voiceOrDefault(String)} and
 * {@code KokoroTTS.resolveVoiceName} exist to absorb.
 * <p>
 * Voice prefix key, for the groups the cast still draws on:
 *   af_ = American Female   am_ = American Male
 *   bf_ = British Female    bm_ = British Male
 *   ef_ = Spanish Female    em_ = Spanish Male
 *   ff_ = French Female
 *   hf_ = Hindi Female      hm_ = Hindi Male
 *   if_ = Italian Female    im_ = Italian Male
 *   pf_ = Portuguese Female pm_ = Portuguese Male
 * <p>
 * The model also ships Japanese (jf_ / jm_) and Chinese (zf_ / zm_) speakers; both groups are held out.
 * <p>
 * The fleet grid renders each voice by its raw enum name; voices are not localized.
 * <p>
 * Source: https://k2-fsa.github.io/sherpa/onnx/tts/pretrained_models/kokoro.html
 */
public enum KokoroVoices {

    // --- American Female (af_) ---
    ALLOY(0, false, "Alloy", "American female"),
    AOEDE(1, false, "Aoede", "American female"),
    BELLA(2, false, "Bella", "American female"),
    HEART(3, false, "Heart", "American female"),
    JESSICA(4, false, "Jessica", "American female"),
    KORE(5, false, "Kore", "American female"),
    //NICOLE(6, false, "Nicole", "American female - whispering"),
    NOVA(7, false, "Nova", "American female"),
    RIVER(8, false, "River", "American female"),
    SARAH(9, false, "Sarah", "American female"),
    SKY(10, false, "Sky", "American female"),

    // --- American Male (am_) ---
    ADAM(11, true, "Adam", "American male"),
    ECHO(12, true, "Echo", "American male"),
    ERIC(13, true, "Eric", "American male"),
    FENRIR(14, true, "Fenrir", "American male"),
    LIAM(15, true, "Liam", "American male"),
    MICHAEL(16, true, "Michael", "American male"),
    ONYX(17, true, "Onyx", "American male"),
    PUCK(18, true, "Puck", "American male"),
    //SANTA_AM(19, true, "Santa", "American male"),

    // --- British Female (bf_) ---
    ALICE(20, false, "Alice", "British female"),
    EMMA(21, false, "Emma", "British female"),
    ISABELLA(22, false, "Isabella", "British female"),
    LILY(23, false, "Lily", "British female"),

    // --- British Male (bm_) ---
    DANIEL(24, true, "Daniel", "British male"),
    FABLE(25, true, "Fable", "British male"),
    GEORGE(26, true, "George", "British male"),
    LEWIS(27, true, "Lewis", "British male"),

    // --- Spanish (ef_ / em_) ---
    ES_DORA(28, false, "Dora", "Spanish female"),
    ES_ALEX(29, true, "Alex", "Spanish male"),

    // --- French (ff_) ---
    FR_SIWIS(30, false, "Siwis", "French female"),

    // --- Hindi (hf_ / hm_) ---
    HI_ALPHA(31, false, "Alpha", "Hindi female"),
    HI_BETA(32, false, "Beta", "Hindi female"),
    HI_OMEGA(33, true, "Omega", "Hindi male"),
    HI_PSI(34, true, "Psi", "Hindi male"),

    // --- Italian (if_ / im_) ---
    IT_SARA(35, false, "Sara", "Italian female"),
    IT_NICOLA(36, true, "Nicola", "Italian male"),

    // --- Japanese (jf_ / jm_) ---
//    JA_ALPHA(37, false, "Alpha", "Japanese female"),
//    JA_GONGITSUNE(38, false, "Gongitsune", "Japanese female"),
//    JA_NEZUMI(39, false, "Nezumi", "Japanese female"),
//    JA_TEBUKURO(40, false, "Tebukuro", "Japanese female"),
//    JA_KUMO(41, true, "Kumo", "Japanese male"),

    // --- Portuguese (pf_ / pm_) ---
    PT_DORA(42, false, "Dora", "Portuguese female"),
    PT_ALEX(43, true, "Alex", "Portuguese male");
    //PT_SANTA(44, true, "Santa", "Portuguese male");

    // --- Chinese (zf_ / zm_) ---
//    ZH_XIAOBEI(45, false, "Xiaobei", "Chinese female"),
//    ZH_XIAONI(46, false, "Xiaoni", "Chinese female"),
//    ZH_XIAOXIAO(47, false, "Xiaoxiao", "Chinese female"),
//    ZH_XIAOYI(48, false, "Xiaoyi", "Chinese female"),
//    ZH_YUNJIAN(49, true, "Yunjian", "Chinese male"),
//    ZH_YUNXI(50, true, "Yunxi", "Chinese male"),
//    ZH_YUNXIA(51, false, "Yunxia", "Chinese female"),
//    ZH_YUNYANG(52, true, "Yunyang", "Chinese male");

    /**
     * The default ship voice, used when a ship has no stored voice or carries a name this engine does not
     * know (see {@link #voiceOrDefault(String)}). It is female because that is what every existing fleet
     * already sounds like; the commander may pick any voice here, male or female.
     */
    public static final KokoroVoices DEFAULT_VOICE = ISABELLA;

    /**
     * A voice for the next radio transmission: any speaker in the model, male or female and in any of its
     * accents, because the voice on the other end of a comms link is a stranger and the variety is the point.
     * The commander's own voice is excluded so the two speakers never sound like the same person.
     *
     * @param ownVoiceName the commander's ship voice (an enum name), or {@code null} when none is resolvable
     */
    public static KokoroVoices randomRadioVoice(String ownVoiceName) {
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
    public static KokoroVoices randomRadioVoice(String ownVoiceName, Set<String> reserved) {
        KokoroVoices[] pool = Arrays.stream(values())
                .filter(voice -> !voice.name().equals(ownVoiceName))
                .filter(voice -> reserved == null || !reserved.contains(voice.name()))
                .toArray(KokoroVoices[]::new);
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
    public static KokoroVoices radioVoiceFor(String speaker, String ownVoiceName, Set<String> reserved) {
        if (speaker == null || speaker.isBlank()) return randomRadioVoice(ownVoiceName, reserved);
        KokoroVoices[] cast = values();
        int start = Math.floorMod(speaker.trim().toLowerCase(Locale.ROOT).hashCode(), cast.length);
        for (int step = 0; step < cast.length; step++) {
            KokoroVoices candidate = cast[(start + step) % cast.length];
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
     * commander's choice, and that choice also decides how the companion refers to herself or himself (see
     * {@code SystemSession.getVoiceGender()}). An unknown name (a voice belonging to another engine) or
     * {@code null} collapses to the default. This is the ship-voice seam only; radio picks from
     * {@link #values()} directly and must not route through here.
     */
    public static KokoroVoices voiceOrDefault(String name) {
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

    KokoroVoices(int sid, boolean male, String displayName, String description) {
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
