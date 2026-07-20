package elite.intel.ai.brain.vega.speech;

import elite.intel.ai.brain.vega.model.Urgency;
import elite.intel.ai.brain.vega.model.speech.SpeechRequest;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.mouth.subscribers.events.TTSInterruptEvent;
import elite.intel.ai.mouth.subscribers.events.VocalisationRequestEvent;
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
 * Interruption: {@code URGENT} speech preempts whatever is currently playing via a {@link TTSInterruptEvent}
 * before enqueueing. Cancelling the returned future targets that exact request. The active Mouth owns the
 * authoritative speaking-state transitions after it synchronously claims the published request.
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
        done.whenComplete((v, ex) -> {
            if (done.isCancelled() || ex instanceof CancellationException) {
                publisher.accept(new TTSInterruptEvent(request.requestId()));
            }
        });
        // Urgent speech jumps ahead of whatever is currently playing.
        if (request.urgency() == Urgency.URGENT) {
            publisher.accept(new TTSInterruptEvent());
        }
        VocalisationRequestEvent event = VocalisationRequestEvent.tracked(
                request.requestId(), request.text(), ORIGIN, true, done);
        try {
            // Same-thread EventBus delivery may still be reentrant/queued. Check after the complete current
            // dispatch so a Mouth receiving this from inside another subscriber gets its chance to claim.
            publisher.accept(event);
            GameEventBus.afterCurrentDispatch(() -> event.handle().rejectIfUnclaimed(new IllegalStateException(
                    "No active Mouth accepted speech request " + request.requestId())));
        } catch (RuntimeException failure) {
            if (!event.handle().rejectIfUnclaimed(failure)) {
                event.handle().fail(failure);
            }
        }
        return done;
    }
}
