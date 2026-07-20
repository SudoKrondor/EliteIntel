package elite.intel.ai.brain.vega.speech;

import elite.intel.ai.brain.vega.model.speech.SpeechRequest;

import java.util.concurrent.CompletableFuture;

/**
 * The single door to text-to-speech for companion mode. Queues {@code SpeechRequest}s (never
 * {@code Thought}s) and handles urgent/barge-in interruption of current speech.
 */
public interface SpeechGateway {

    /**
     * Submits text for vocalization.
     *
     * @return a future completing when the utterance finishes; cancel it to skip (if queued) or
     *         stop (if currently speaking)
     */
    CompletableFuture<Void> submit(SpeechRequest request);
}
