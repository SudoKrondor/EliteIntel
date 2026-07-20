package elite.intel.vega.speech;

import elite.intel.vega.CompanionRuntimeGeneration;
import elite.intel.vega.model.Urgency;
import elite.intel.vega.model.speech.SpeechRequest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerationBoundSpeechGatewayTest {

    @Test
    void closeCancelsOwnedUtteranceAndRejectsNewSpeech() {
        RecordingSpeechGateway delegate = new RecordingSpeechGateway();
        GenerationBoundSpeechGateway gateway = new GenerationBoundSpeechGateway(
                delegate, new CompanionRuntimeGeneration());

        CompletableFuture<Void> pendingUtterance = gateway.submit(request("utterance-1"));
        gateway.close();
        CompletableFuture<Void> rejectedUtterance = gateway.submit(request("utterance-2"));

        assertTrue(pendingUtterance.isCancelled());
        assertTrue(rejectedUtterance.isCancelled());
        assertEquals(1, delegate.submissions);
    }

    private static SpeechRequest request(String requestId) {
        return new SpeechRequest(requestId, "Course plotted.", Urgency.NORMAL);
    }

    private static final class RecordingSpeechGateway implements SpeechGateway {
        private final CompletableFuture<Void> pendingUtterance = new CompletableFuture<>();
        private int submissions;

        @Override
        public CompletableFuture<Void> submit(SpeechRequest request) {
            submissions++;
            return pendingUtterance;
        }
    }
}
