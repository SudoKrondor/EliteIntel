package elite.intel.ai.mouth.subscribers.events;

import elite.intel.i18n.Language;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Set;

public class RadioTransmissionEvent extends BaseVoxEvent {

    private final Language textLanguage;
    private final String source;
    private final String voiceName;
    private final String speakerKey;
    private final Set<String> reservedVoices;

    public RadioTransmissionEvent(String textToVoice, Language textLanguage, @Nullable String source,
                                  @Nullable String voiceName, Set<String> reservedVoices) {
        this(textToVoice, textLanguage, source, voiceName, reservedVoices, null);
    }

    /**
     * @param textLanguage   the language {@code textToVoice} is written in, which the engine reads it in. Supplied
     *                       by the publisher because only it knows whose words these are: a line relayed from
     *                       the journal is in the game client's language, while one we compose ourselves - the
     *                       docking grant - is in the commander's, and a radio line is not one or the other by
     *                       nature. Read in the wrong one, Italian came out with an English accent and English
     *                       numbers.
     * @param source         localized name of whoever is transmitting - a station, a fleet carrier, another
     *                       commander. Carried alongside the text because the chat log and the HUD overlay
     *                       attribute what they print, and a transmission is not the AI talking.
     * @param voiceName      the radio-engine voice this speaker has been given, or null to draw a stranger at
     *                       random. Only the commander's own carriers can be assigned one: everyone else on the
     *                       channel is a voice they have never heard before, which is what makes it a channel.
     * @param reservedVoices voices that belong to a named speaker and so must not be drawn for this one.
     *                       Supplied by the publisher rather than looked up here: the mouth knows about
     *                       engines and voices, and nothing about carriers.
     * @param speakerKey     who is transmitting, as one particular individual the commander will hear again - an
     *                       NPC pilot's name. Null for anyone the channel has no individual behind: a station, a
     *                       police wing, a construction site. A key keeps that speaker on one voice; without one
     *                       the engine draws a stranger per transmission, which is what those senders are.
     */
    public RadioTransmissionEvent(String textToVoice, Language textLanguage, @Nullable String source,
                                  @Nullable String voiceName, Set<String> reservedVoices,
                                  @Nullable String speakerKey) {
        super(textToVoice, true);
        this.textLanguage = Objects.requireNonNull(textLanguage, "textLanguage");
        this.source = source;
        this.voiceName = voiceName;
        this.speakerKey = speakerKey;
        this.reservedVoices = reservedVoices == null ? Set.of() : Set.copyOf(reservedVoices);
    }

    public Language getTextLanguage() {
        return textLanguage;
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
