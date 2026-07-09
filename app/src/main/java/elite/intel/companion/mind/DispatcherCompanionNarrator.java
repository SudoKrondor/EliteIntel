package elite.intel.companion.mind;

import elite.intel.companion.CompanionNarrator;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.companion.speech.SpeechGateway;

import java.util.UUID;

/**
 * Production {@link CompanionNarrator}: routes a subscriber's voicing request to the right owner - a
 * start-of-processing filler straight to the {@link SpeechGateway} (voiced, not remembered), and a narrated or
 * verbatim result to the {@link ThoughtDispatcher}'s EVENT lane (voiced and remembered as a {@code user ->
 * assistant} pair). It holds no state of its own; it is the thin composition point installed into
 * {@code CompanionRuntime} so gameplay subscribers reach the companion through one door.
 */
public final class DispatcherCompanionNarrator implements CompanionNarrator {

    private final ThoughtDispatcher dispatcher;
    private final SpeechGateway speech;

    public DispatcherCompanionNarrator(ThoughtDispatcher dispatcher, SpeechGateway speech) {
        this.dispatcher = dispatcher;
        this.speech = speech;
    }

    @Override
    public void filler(String text, boolean urgent) {
        if (text == null || text.isBlank()) {
            return;
        }
        // Start-of-processing throwaway line: straight to TTS, no thought and no memory (it carries no
        // information worth remembering).
        speech.submit(new SpeechRequest(UUID.randomUUID().toString(), text, urgency(urgent)));
    }

    @Override
    public void narrate(String data, String instructions, String topic) {
        dispatcher.submitEventReaction(data, instructions, topic, Urgency.NORMAL);
    }

    @Override
    public void announce(String sourceId, String phrase, String topic, boolean urgent) {
        dispatcher.submitEventVerbatim(sourceId, phrase, topic, urgency(urgent));
    }

    private static Urgency urgency(boolean urgent) {
        return urgent ? Urgency.URGENT : Urgency.NORMAL;
    }
}
