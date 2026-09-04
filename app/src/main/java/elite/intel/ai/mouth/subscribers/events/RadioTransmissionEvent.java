package elite.intel.ai.mouth.subscribers.events;

import javax.annotation.Nullable;
import java.util.Set;

public class RadioTransmissionEvent extends BaseVoxEvent {

    private final String source;
    private final String voiceName;
    private final Set<String> reservedVoices;

    /**
     * @param source localized name of whoever is transmitting - a station, a fleet carrier, another
     *               commander. Carried alongside the text because the chat log and the HUD overlay
     *               attribute what they print, and a transmission is not the AI talking.
     */
    public RadioTransmissionEvent(String textToVoice, @Nullable String source) {
        this(textToVoice, source, null, Set.of());
    }

    public RadioTransmissionEvent(String textToVoice, @Nullable String source, @Nullable String voiceName) {
        this(textToVoice, source, voiceName, Set.of());
    }

    /**
     * @param voiceName the radio-engine voice this speaker has been given, or null to draw a stranger at
     *                  random. Only the commander's own carriers can be assigned one: everyone else on the
     *                  channel is a voice they have never heard before, which is what makes it a channel.
     */
    /**
     * @param reservedVoices voices that belong to a named speaker and so must not be drawn for this one.
     *                       Supplied by the publisher rather than looked up here: the mouth knows about
     *                       engines and voices, and nothing about carriers.
     */
    public RadioTransmissionEvent(String textToVoice, @Nullable String source, @Nullable String voiceName,
                                  Set<String> reservedVoices) {
        super(textToVoice, true);
        this.source = source;
        this.voiceName = voiceName;
        this.reservedVoices = reservedVoices == null ? Set.of() : Set.copyOf(reservedVoices);
    }

    public @Nullable String getSource() {
        return source;
    }

    public @Nullable String getVoiceName() {
        return voiceName;
    }

    public Set<String> getReservedVoices() {
        return reservedVoices;
    }
}
