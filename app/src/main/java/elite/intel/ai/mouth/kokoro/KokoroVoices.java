package elite.intel.ai.mouth.kokoro;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Speaker IDs for the kokoro-multi-lang-v1_0 model (53 voices).
 * <p>
 * Voice prefix key:
 *   af_ = American Female   am_ = American Male
 *   bf_ = British Female    bm_ = British Male
 *   ef_ = Spanish Female    em_ = Spanish Male
 *   ff_ = French Female
 *   hf_ = Hindi Female      hm_ = Hindi Male
 *   if_ = Italian Female    im_ = Italian Male
 *   jf_ = Japanese Female   jm_ = Japanese Male
 *   pf_ = Portuguese Female pm_ = Portuguese Male
 *   zf_ = Chinese Female    zm_ = Chinese Male
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
    NICOLE(6, false, "Nicole", "American female - whispering"),
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
    SANTA_AM(19, true, "Santa", "American male"),

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
    JA_ALPHA(37, false, "Alpha", "Japanese female"),
    JA_GONGITSUNE(38, false, "Gongitsune", "Japanese female"),
    JA_NEZUMI(39, false, "Nezumi", "Japanese female"),
    JA_TEBUKURO(40, false, "Tebukuro", "Japanese female"),
    JA_KUMO(41, true, "Kumo", "Japanese male"),

    // --- Portuguese (pf_ / pm_) ---
    PT_DORA(42, false, "Dora", "Portuguese female"),
    PT_ALEX(43, true, "Alex", "Portuguese male"),
    PT_SANTA(44, true, "Santa", "Portuguese male"),

    // --- Chinese (zf_ / zm_) ---
    ZH_XIAOBEI(45, false, "Xiaobei", "Chinese female"),
    ZH_XIAONI(46, false, "Xiaoni", "Chinese female"),
    ZH_XIAOXIAO(47, false, "Xiaoxiao", "Chinese female"),
    ZH_XIAOYI(48, false, "Xiaoyi", "Chinese female"),
    ZH_YUNJIAN(49, true, "Yunjian", "Chinese male"),
    ZH_YUNXI(50, true, "Yunxi", "Chinese male"),
    ZH_YUNXIA(51, false, "Yunxia", "Chinese female"),
    ZH_YUNYANG(52, true, "Yunyang", "Chinese male");

    /**
     * Default female voice. Ship voices are female-only (see {@link #femaleOrDefault(String)}); a ship with
     * no stored voice, an unknown voice, or a legacy male voice resolves to this. Radio transmissions are a
     * separate channel and still draw from the full {@link #values()} set (male and female alike).
     */
    public static final KokoroVoices DEFAULT_FEMALE = BELLA;

    /**
     * A voice for the next radio transmission: any speaker in the model, male or female and in any of its
     * accents, because the voice on the other end of a comms link is a stranger and the variety is the point.
     * The commander's own voice is excluded so the two speakers never sound like the same person.
     *
     * @param ownVoiceName the commander's ship voice (an enum name), or {@code null} when none is resolvable
     */
    public static KokoroVoices randomRadioVoice(String ownVoiceName) {
        KokoroVoices[] pool = Arrays.stream(values())
                .filter(voice -> !voice.name().equals(ownVoiceName))
                .toArray(KokoroVoices[]::new);
        return pool.length == 0
                ? DEFAULT_FEMALE
                : pool[ThreadLocalRandom.current().nextInt(pool.length)];
    }

    /**
     * Resolves a stored ship-voice name to a female voice: the named voice when it is a valid female voice,
     * otherwise {@link #DEFAULT_FEMALE}. Ship voices are female-only, so a male name (a legacy selection from
     * before that constraint) or an unknown/{@code null} name collapses to the default female. This is the
     * ship-voice seam only; radio picks from {@link #values()} directly and must not route through here.
     */
    public static KokoroVoices femaleOrDefault(String name) {
        if (name == null) return DEFAULT_FEMALE;
        try {
            KokoroVoices v = valueOf(name);
            return v.isMale() ? DEFAULT_FEMALE : v;
        } catch (IllegalArgumentException e) {
            return DEFAULT_FEMALE;
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
