package elite.intel.ui.event;

import javax.annotation.Nullable;

public class AiResponseLogEvent {
    private final String data;
    private final String speaker;

    public AiResponseLogEvent(String data) {
        this(data, null);
    }

    /**
     * @param speaker who is talking, when it is not the AI: the localized name of a radio
     *                transmission's source (a station, a carrier, another commander). Null means
     *                the AI itself, which the UI names after the commander's ship.
     */
    public AiResponseLogEvent(String data, @Nullable String speaker) {
        this.data = data;
        this.speaker = speaker;
    }

    public String getData() {
        return data;
    }

    /**
     * The transmission's source, or null when the AI is the speaker.
     */
    public @Nullable String getSpeaker() {
        return speaker;
    }
}
