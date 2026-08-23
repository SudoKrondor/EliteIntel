package elite.intel.ai.mouth;

/**
 * The gender the companion is heard as, derived from the voice the active ship carries.
 * <p>
 * The voice is the only thing a commander picks, and it decides two separate things: which speaker the TTS
 * engine synthesises, and how the companion refers to herself or himself in speech. Those used to be
 * independent - every ship voice was forced female and the prompts said "feminine" as a constant - so a fleet
 * that could only sound female was the only fleet the prompts could describe. Now the fleet grid offers every
 * voice each engine has, and this enum is the one seam that carries that choice into the prompt
 * (see {@code SystemSession.getVoiceGender()}).
 * <p>
 * {@link #FEMALE} is the value an unknown or unset voice resolves to, matching the default voice of all three
 * engines, so nothing about an untouched install changes.
 */
public enum VoiceGender {
    FEMALE,
    MALE;

    public static VoiceGender of(boolean male) {
        return male ? MALE : FEMALE;
    }

    public boolean isMale() {
        return this == MALE;
    }
}
