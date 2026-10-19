package elite.intel.ai.mouth.piper;

import elite.intel.ai.mouth.sherpa.RadioVoiceDraw;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

/** Voice IDs from the French Piper UPMC and Siwis medium models. */
public enum PiperVoices {
    UPMC_JESSICA(0, false, false, "Jessica", "French · UPMC"),
    UPMC_PIERRE(1, true, false, "Pierre", "French · UPMC"),
    SIWIS(0, false, true, "Siwis", "French · Siwis");

    public static final PiperVoices DEFAULT_VOICE = UPMC_JESSICA;

    private final int sid;
    private final boolean male;
    private final boolean siwisModel;
    private final String displayName;
    private final String description;

    PiperVoices(int sid, boolean male, boolean siwisModel, String displayName, String description) {
        this.sid = sid;
        this.male = male;
        this.siwisModel = siwisModel;
        this.displayName = displayName;
        this.description = description;
    }

    public int getSid() {
        return sid;
    }

    public boolean isMale() {
        return male;
    }

    public boolean usesSiwisModel() {
        return siwisModel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static PiperVoices voiceOrDefault(String name) {
        if (name == null) return DEFAULT_VOICE;
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return DEFAULT_VOICE;
        }
    }

    public static Stream<PiperVoices> roster() {
        return Arrays.stream(values());
    }

    public static PiperVoices radioVoiceFor(String speaker, String ownVoiceName, Set<String> reserved) {
        PiperVoices[] cast = roster().toArray(PiperVoices[]::new);
        return RadioVoiceDraw.forSpeaker(cast, DEFAULT_VOICE, speaker, ownVoiceName, reserved);
    }
}
