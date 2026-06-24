package elite.intel.companion.speech;

import elite.intel.ai.ears.IsSpeakingEvent;
import elite.intel.ai.mouth.subscribers.events.TTSInterruptEvent;
import elite.intel.ai.mouth.subscribers.events.VocalisationRequestEvent;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.speech.SpeechRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the gateway wires a {@link SpeechRequest} onto the existing TTS event pipeline: it brackets
 * the utterance with {@link IsSpeakingEvent} for STT suppression, hands the Mouth a completion future it
 * returns to the caller, preempts current speech for urgent requests, and best-effort hard-stops on cancel.
 */
class CompanionSpeechGatewayTest {

    private final List<Object> published = new ArrayList<>();
    private final CompanionSpeechGateway gateway = new CompanionSpeechGateway(published::add);

    private VocalisationRequestEvent vocalisation() {
        return published.stream()
                .filter(VocalisationRequestEvent.class::isInstance)
                .map(VocalisationRequestEvent.class::cast)
                .findFirst().orElseThrow();
    }

    @Test
    void normalSpeechBracketsSpeakingAndReturnsMouthFuture() {
        CompletableFuture<Void> result = gateway.submit(new SpeechRequest("r1", "docking clamps released", Urgency.NORMAL));

        // Speaking guard is raised before the utterance is enqueued.
        assertInstanceOf(IsSpeakingEvent.class, published.get(0));
        assertTrue(((IsSpeakingEvent) published.get(0)).isSpeaking());

        // No urgent preemption for normal speech.
        assertFalse(published.stream().anyMatch(TTSInterruptEvent.class::isInstance));

        VocalisationRequestEvent event = vocalisation();
        assertEquals("docking clamps released", event.getText());
        assertTrue(event.canBeInterrupted());
        // The future the caller gets is exactly the one the Mouth completes.
        assertSame(result, event.getCompletionFuture());
        assertFalse(result.isDone());
    }

    @Test
    void mouthCompletionLowersSpeakingGuard() {
        CompletableFuture<Void> result = gateway.submit(new SpeechRequest("r1", "hello", Urgency.NORMAL));

        vocalisation().getCompletionFuture().complete(null);

        assertTrue(result.isDone());
        IsSpeakingEvent last = published.stream()
                .filter(IsSpeakingEvent.class::isInstance)
                .map(IsSpeakingEvent.class::cast)
                .reduce((a, b) -> b).orElseThrow();
        assertFalse(last.isSpeaking());
    }

    @Test
    void urgentSpeechPreemptsCurrentSpeech() {
        gateway.submit(new SpeechRequest("r1", "incoming fire", Urgency.URGENT));

        // Interrupt is emitted before the utterance is enqueued.
        int interruptIndex = indexOf(TTSInterruptEvent.class);
        int speechIndex = indexOf(VocalisationRequestEvent.class);
        assertTrue(interruptIndex >= 0 && interruptIndex < speechIndex);
    }

    @Test
    void cancelHardStopsCurrentSpeech() {
        CompletableFuture<Void> result = gateway.submit(new SpeechRequest("r1", "long story", Urgency.NORMAL));

        result.cancel(true);

        assertTrue(published.stream().anyMatch(TTSInterruptEvent.class::isInstance));
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
