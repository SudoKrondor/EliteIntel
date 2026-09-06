package elite.intel.ai.mouth.subscribers.events;

import javax.annotation.Nullable;
import java.util.Set;

public class RadioTransmissionEvent extends BaseVoxEvent {

    private final String source;
    private final String voiceName;
    private final String speakerKey;
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

    public RadioTransmissionEvent(String textToVoice, @Nullable String source, @Nullable String voiceName,
                                  Set<String> reservedVoices) {
        this(textToVoice, source, voiceName, reservedVoices, null);
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
    /**
     * @param speakerKey who is transmitting, as one particular individual the commander will hear again - an
     *                   NPC pilot's name. Null for anyone the channel has no individual behind: a station, a
     *                   police wing, a construction site. A key keeps that speaker on one voice; without one
     *                   the engine draws a stranger per transmission, which is what those senders are.
     */
    public RadioTransmissionEvent(String textToVoice, @Nullable String source, @Nullable String voiceName,
                                  Set<String> reservedVoices, @Nullable String speakerKey) {
        super(textToVoice, true);
        this.source = source;
        this.voiceName = voiceName;
        this.speakerKey = speakerKey;
        this.reservedVoices = reservedVoices == null ? Set.of() : Set.copyOf(reservedVoices);
    }

    public @Nullable String getSource() {
        return source;
    }

    public @Nullable String getVoiceName() {
        return voiceName;
    }

    public @Nullable String getSpeakerKey() {
        return speakerKey;
    }

    public Set<String> getReservedVoices() {
        return reservedVoices;
    }
}
