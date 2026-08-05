package elite.intel.ai.mouth.subscribers.events;

import javax.annotation.Nullable;

public class RadioTransmissionEvent extends BaseVoxEvent {

    private final String source;

    /**
     * @param source localized name of whoever is transmitting - a station, a fleet carrier, another
     *               commander. Carried alongside the text because the chat log and the HUD overlay
     *               attribute what they print, and a transmission is not the AI talking.
     */
    public RadioTransmissionEvent(String textToVoice, @Nullable String source) {
        super(textToVoice, true);
        this.source = source;
    }

    public @Nullable String getSource() {
        return source;
    }
}
