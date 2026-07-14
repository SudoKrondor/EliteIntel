package elite.intel.companion.speech;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.TTSInterruptEvent;
import elite.intel.ai.mouth.subscribers.events.VocalisationRequestEvent;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.eventbus.GameEventBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wiring test over the real {@link GameEventBus} (Guava delivers synchronously on the posting thread):
 * proves the production gateway's events actually reach a Mouth-shaped subscriber under the exact event
 * types/getters the real {@code MouthInterface} consumes, and that a barge-in {@link TTSInterruptEvent}
 * resolves the future the gateway handed the caller. The unit test covers the publish logic in isolation;
 * this catches regressions in the cross-bus contract (event type, origin, completion-future plumbing).
 */
class CompanionSpeechGatewayBusIntegrationTest {

    private record SubmitSpeechEvent() {
    }

    /** Minimal stand-in for the real Mouth: records the utterance and completes its future on interrupt. */
    private static final class FakeMouth {
        volatile VocalisationRequestEvent received;
        volatile boolean interrupted;

        @Subscribe
        public void onSpeak(VocalisationRequestEvent event) {
            event.handle().claimForPlayback();
            received = event;
        }

        @Subscribe
        public void onInterrupt(TTSInterruptEvent event) {
            interrupted = true;
            // Mirrors interruptAndClear() draining its queue and completing pending completion futures.
            if (received != null && received.getCompletionFuture() != null) {
                received.getCompletionFuture().complete(null);
            }
        }
    }

    private static final class NestedSpeechSubmitter {
        private final CompanionSpeechGateway gateway;
        private volatile CompletableFuture<Void> completion;

        private NestedSpeechSubmitter(CompanionSpeechGateway gateway) {
            this.gateway = gateway;
        }

        @Subscribe
        public void onSubmit(SubmitSpeechEvent event) {
            completion = gateway.submit(new SpeechRequest("nested", "startup complete", Urgency.NORMAL));
        }
    }

    private final FakeMouth mouth = new FakeMouth();
    private final CompanionSpeechGateway gateway = new CompanionSpeechGateway();
    private final NestedSpeechSubmitter nestedSubmitter = new NestedSpeechSubmitter(gateway);

    @BeforeEach
    void register() {
        GameEventBus.register(mouth);
        GameEventBus.register(nestedSubmitter);
    }

    @AfterEach
    void unregister() {
        GameEventBus.unregister(nestedSubmitter);
        GameEventBus.unregister(mouth);
    }

    @Test
    void utteranceReachesMouthWithCallerFuture() {
        CompletableFuture<Void> result = gateway.submit(new SpeechRequest("r1", "fuel at twelve percent", Urgency.NORMAL));

        assertNotNull(mouth.received, "vocalisation event did not reach the Mouth");
        assertSame(result, mouth.received.getCompletionFuture(), "Mouth must receive the caller's future");
        assertTrue(mouth.received.canBeInterrupted(), "companion speech must be interruptible");
        assertFalse(mouth.interrupted, "normal speech must not preempt");
        mouth.received.handle().complete();
    }

    @Test
    void urgentSpeechPreemptsBeforeReachingMouth() {
        CompletableFuture<Void> result = gateway.submit(
                new SpeechRequest("r1", "collision warning", Urgency.URGENT));

        assertTrue(mouth.interrupted, "urgent speech must emit an interrupt the Mouth sees");
        assertNotNull(mouth.received, "urgent speech must still be enqueued after the interrupt");
        mouth.received.handle().complete();
        assertTrue(result.isDone());
    }

    @Test
    void bargeInInterruptResolvesCallerFuture() {
        CompletableFuture<Void> result = gateway.submit(new SpeechRequest("r1", "a long winded story", Urgency.NORMAL));
        assertFalse(result.isDone());

        // Commander barges in: ears posts a TTSInterruptEvent exactly as ParakeetSTTImpl does.
        GameEventBus.publish(new TTSInterruptEvent());

        assertTrue(result.isDone(), "barge-in must resolve the speech future the caller is waiting on");
    }

    @Test
    void speechSubmittedReentrantlyIsClaimedBeforeTheNoMouthCheck() {
        GameEventBus.publish(new SubmitSpeechEvent());

        assertNotNull(nestedSubmitter.completion);
        assertNotNull(mouth.received);
        assertFalse(nestedSubmitter.completion.isDone(),
                "the queued Mouth event must run before the unclaimed-request check");
        mouth.received.handle().complete();
    }
}
