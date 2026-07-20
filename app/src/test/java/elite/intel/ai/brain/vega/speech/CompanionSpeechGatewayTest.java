package elite.intel.ai.brain.vega.speech;

import elite.intel.ai.mouth.subscribers.events.TTSInterruptEvent;
import elite.intel.ai.mouth.subscribers.events.VocalisationRequestEvent;
import elite.intel.ai.brain.vega.model.Urgency;
import elite.intel.ai.brain.vega.model.speech.SpeechRequest;
import elite.intel.ai.brain.vega.speech.CompanionSpeechGateway;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the gateway wires a {@link SpeechRequest} onto the existing TTS event pipeline, preserves its
 * correlation/completion handle, preempts current speech for urgent requests, targets cancellation, and fails
 * promptly when no active Mouth accepts the synchronous publication.
 */
class CompanionSpeechGatewayTest {

    private final List<Object> published = new ArrayList<>();
    private final CompanionSpeechGateway gateway = new CompanionSpeechGateway(event -> {
        published.add(event);
        if (event instanceof VocalisationRequestEvent request) {
            request.handle().claimForPlayback();
        }
    });

    private VocalisationRequestEvent vocalisation() {
        return published.stream()
                .filter(VocalisationRequestEvent.class::isInstance)
                .map(VocalisationRequestEvent.class::cast)
                .findFirst().orElseThrow();
    }

    @Test
    void normalSpeechPreservesCorrelationAndReturnsMouthFuture() {
        CompletableFuture<Void> result = gateway.submit(new SpeechRequest("r1", "docking clamps released", Urgency.NORMAL));

        // No urgent preemption for normal speech.
        assertFalse(published.stream().anyMatch(TTSInterruptEvent.class::isInstance));

        VocalisationRequestEvent event = vocalisation();
        assertEquals("r1", event.handle().requestId());
        assertEquals("docking clamps released", event.getText());
        assertTrue(event.canBeInterrupted());
        // The future the caller gets is exactly the one the Mouth completes.
        assertSame(result, event.getCompletionFuture());
        assertFalse(result.isDone());
        event.handle().complete();
    }

    @Test
    void mouthCompletionResolvesCallerFuture() {
        CompletableFuture<Void> result = gateway.submit(new SpeechRequest("r1", "hello", Urgency.NORMAL));

        vocalisation().handle().complete();

        assertTrue(result.isDone());
    }

    @Test
    void urgentSpeechPreemptsCurrentSpeech() {
        CompletableFuture<Void> result = gateway.submit(new SpeechRequest("r1", "incoming fire", Urgency.URGENT));

        // Interrupt is emitted before the utterance is enqueued.
        int interruptIndex = indexOf(TTSInterruptEvent.class);
        int speechIndex = indexOf(VocalisationRequestEvent.class);
        assertTrue(interruptIndex >= 0 && interruptIndex < speechIndex);
        vocalisation().handle().complete();
        assertTrue(result.isDone());
    }

    @Test
    void cancelHardStopsCurrentSpeech() {
        CompletableFuture<Void> result = gateway.submit(new SpeechRequest("r1", "long story", Urgency.NORMAL));

        result.cancel(true);

        TTSInterruptEvent interrupt = published.stream()
                .filter(TTSInterruptEvent.class::isInstance)
                .map(TTSInterruptEvent.class::cast)
                .findFirst().orElseThrow();
        assertEquals("r1", interrupt.requestId());
    }

    @Test
    void noActiveMouthFailsImmediately() {
        CompanionSpeechGateway unclaimed = new CompanionSpeechGateway(published::add);

        CompletableFuture<Void> result = unclaimed.submit(
                new SpeechRequest("r1", "anyone there", Urgency.NORMAL));

        assertTrue(result.isCompletedExceptionally());
    }

    @Test
    void blankSpeechIsRejectedBeforePublication() {
        assertThrows(IllegalArgumentException.class,
                () -> new SpeechRequest("r1", "   ", Urgency.NORMAL));
        assertTrue(published.isEmpty());
    }

    private int indexOf(Class<?> type) {
        for (int i = 0; i < published.size(); i++) {
            if (type.isInstance(published.get(i))) {
                return i;
            }
        }
        return -1;
    }
}
