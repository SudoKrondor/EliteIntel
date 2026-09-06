package elite.intel.ui.event;

import elite.intel.ui.i18n.DisplayNumerals;

import javax.annotation.Nullable;

/**
 * A line the commander has just heard, for the chat log and the HUD overlay to show.
 * <p>
 * The text arrives as it was spoken, with every figure spelled out in words so the TTS engine could not
 * mangle a group separator. Reading is not listening, so the figures go back to digits here, in the
 * commander's own number format - see {@link DisplayNumerals}. It happens once, at the event every display
 * of spoken text goes through, rather than in each of the three engines that publish one.
 */
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
        this.data = DisplayNumerals.digits(data);
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
