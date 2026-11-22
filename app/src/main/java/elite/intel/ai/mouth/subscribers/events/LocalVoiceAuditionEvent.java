package elite.intel.ai.mouth.subscribers.events;

/**
 * Plays one Supertonic voice, clean, while the commander tunes it. Supertonic speaks it whichever role it is in -
 * the main mouth, or the radio engine beside Google - because the voice being tuned is always one of its own.
 */
public class LocalVoiceAuditionEvent extends BaseVoxEvent {

    private final String voiceName;

    /**
     * @param voiceName a {@link elite.intel.ai.mouth.supertonic.SupertonicVoices} constant name
     */
    public LocalVoiceAuditionEvent(String textToVoice, String voiceName) {
        super(textToVoice, false);
        this.voiceName = voiceName;
    }

    public String getVoiceName() {
        return voiceName;
    }
}
