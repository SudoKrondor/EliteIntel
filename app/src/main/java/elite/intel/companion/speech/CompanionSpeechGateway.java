package elite.intel.companion.speech;

import elite.intel.ai.ears.IsSpeakingEvent;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.mouth.subscribers.events.TTSInterruptEvent;
import elite.intel.ai.mouth.subscribers.events.VocalisationRequestEvent;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.eventbus.GameEventBus;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * {@link SpeechGateway} backed by the existing TTS pipeline. A {@link SpeechRequest} is published as a
 * {@link VocalisationRequestEvent} carrying a completion future that the active {@code MouthInterface}
 * (Google or Kokoro) completes when playback finishes (or when speech is interrupted/drained); that
 * future is returned to the caller, so the gateway is provider-agnostic and never touches the audio.
 * <p>
 * STT suppression: the gateway brackets each utterance with {@link IsSpeakingEvent} (true before, false
 * after), mirroring how {@code VocalisationRouter} guards normal AI responses, so voice recognition does
 * not pick up the companion's own voice.
 * <p>
 * Interruption: {@code URGENT} speech preempts whatever is currently playing via a {@link TTSInterruptEvent}
 * before enqueueing. Cancelling the returned future best-effort stops current speech the same way.
 */
public final class CompanionSpeechGateway implements SpeechGateway {

    /** Origin marker on the vocalisation event; the companion voice is an AI response to the commander. */
    private static final Class<AiVoxResponseEvent> ORIGIN = AiVoxResponseEvent.class;

    private final Consumer<Object> publisher;

    /** Production constructor: publishes on the shared {@link GameEventBus}. */
    public CompanionSpeechGateway() {
        this(GameEventBus::publish);
    }

    /** Test seam: inject a capturing publisher to avoid the real event bus. */
    CompanionSpeechGateway(Consumer<Object> publisher) {
        this.publisher = publisher;
    }

    @Override
    public CompletableFuture<Void> submit(SpeechRequest request) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        publisher.accept(new IsSpeakingEvent(true));
        // Lift the speaking guard when playback ends; a cancel also hard-stops any current speech.
        done.whenComplete((v, ex) -> {
            publisher.accept(new IsSpeakingEvent(false));
            if (ex instanceof CancellationException) {
                publisher.accept(new TTSInterruptEvent());
            }
        });
        // Urgent speech jumps ahead of whatever is currently playing.
        if (request.urgency() == Urgency.URGENT) {
            publisher.accept(new TTSInterruptEvent());
        }
        // Interruptible so barge-in / urgent preemption can clear it; the Mouth completes `done` when finished.
        publisher.accept(new VocalisationRequestEvent(request.text(), ORIGIN, true, done));
        return done;
    }
}
