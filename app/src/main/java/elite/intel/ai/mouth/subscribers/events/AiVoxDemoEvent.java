package elite.intel.ai.mouth.subscribers.events;

public class AiVoxDemoEvent extends BaseVoxEvent {

    private final String voiceName;
    private final boolean radio;

    public AiVoxDemoEvent(String textToVoice) {
        this(textToVoice, "");
    }

    public AiVoxDemoEvent(String textToVoice, String voiceName) {
        this(textToVoice, voiceName, false);
    }

    /**
     * @param radio true to audition the voice the way it will actually be heard - through the radio engine
     *              and its transmission filter. A carrier's traffic control is never voiced by the main
     *              mouth, so auditioning its voice there would demonstrate the wrong engine.
     */
    public AiVoxDemoEvent(String textToVoice, String voiceName, boolean radio) {
        super(textToVoice, false);
        this.voiceName = voiceName;
        this.radio = radio;
    }

    public String getVoiceName() {
        return voiceName;
    }

    public boolean isRadio() {
        return radio;
    }
}
