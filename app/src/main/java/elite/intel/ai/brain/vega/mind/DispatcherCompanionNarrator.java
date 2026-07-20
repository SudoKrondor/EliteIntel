package elite.intel.ai.brain.vega.mind;

import elite.intel.ai.brain.vega.CompanionNarrator;
import elite.intel.ai.brain.vega.CompanionRuntimeGeneration;
import elite.intel.ai.brain.vega.model.Urgency;
import elite.intel.ai.brain.vega.model.speech.SpeechRequest;
import elite.intel.ai.brain.vega.speech.SpeechGateway;

import java.util.UUID;

/**
 * Production {@link CompanionNarrator}: routes a subscriber's voicing request to the right owner - a
 * start-of-processing filler straight to the {@link SpeechGateway} (voiced, not remembered), and a narrated or
 * verbatim result to the {@link ThoughtDispatcher}'s EVENT lane (voiced and stored as a completed EVENT record).
 * It holds no state of its own; it is the thin composition point installed into
 * {@code CompanionRuntime} so gameplay subscribers reach the companion through one door.
 */
public final class DispatcherCompanionNarrator implements CompanionNarrator {

    private final ThoughtDispatcher dispatcher;
    private final SpeechGateway speech;
    private final CompanionRuntimeGeneration runtimeGeneration;

    public DispatcherCompanionNarrator(
            ThoughtDispatcher dispatcher,
            SpeechGateway speech,
            CompanionRuntimeGeneration runtimeGeneration
    ) {
        this.dispatcher = dispatcher;
        this.speech = speech;
        this.runtimeGeneration = runtimeGeneration;
    }

    @Override
    public void filler(String text, boolean urgent) {
        if (!runtimeGeneration.isActive() || text == null || text.isBlank()) {
            return;
        }
        // Start-of-processing throwaway line: straight to TTS, no thought and no memory (it carries no
        // information worth remembering).
        speech.submit(new SpeechRequest(UUID.randomUUID().toString(), text, urgency(urgent)));
    }

    @Override
    public void narrate(String data, String instructions) {
        if (runtimeGeneration.isActive()) {
            dispatcher.submitEventReaction(data, instructions, Urgency.NORMAL);
        }
    }

    @Override
    public void announce(String phrase, boolean urgent) {
        if (runtimeGeneration.isActive()) {
            dispatcher.submitEventVerbatim(phrase, urgency(urgent));
        }
    }

    private static Urgency urgency(boolean urgent) {
        return urgent ? Urgency.URGENT : Urgency.NORMAL;
    }
}
