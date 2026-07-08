package elite.intel.diagnostics;

import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.companion.speech.CompanionSpeechGateway;
import elite.intel.companion.speech.SpeechGateway;

import java.util.concurrent.CompletableFuture;

/**
 * Diagnostics-mode {@link SpeechGateway}: a thin decorator over the real {@link CompanionSpeechGateway} so the
 * companion voice is fully audible and the chat panel shows every reply, exactly as in normal operation - the
 * harness must be indistinguishable from a live session for everything the operator sees and hears. It only
 * skips the microphone (STT), never the speaker.
 * <p>
 * Delegating to the production gateway means the reply travels the real TTS path: {@code IsSpeakingEvent}
 * brackets (which drive the {@code DIAG speaking} markers and the pacer's settle detection), a
 * {@link elite.intel.ai.mouth.subscribers.events.VocalisationRequestEvent} the active Mouth voices, and the
 * {@code AiResponseLogEvent} that Mouth publishes to populate the chat panel. The returned future is the real
 * playback future, so a caller that blocks on speech (a bridged narration's {@code spokenSignal}, the mid-term
 * consolidator) waits for actual audio just like in production.
 * <p>
 * Additionally pings {@link DiagnosticsPacer#markActivity()} so a speak-only turn (pure conversation, or a
 * query answer - neither emits a {@code DIAG dispatch}) registers turn activity even in the brief window
 * before {@code IsSpeakingEvent(true)} arrives; without it such a turn could momentarily look idle to the
 * pacer's start-grace check.
 */
public final class DiagnosticsSpeechGateway implements SpeechGateway {

    private final SpeechGateway delegate = new CompanionSpeechGateway();

    @Override
    public CompletableFuture<Void> submit(SpeechRequest request) {
        DiagnosticsPacer.getInstance().markActivity();
        // Real TTS path: audible voice, chat-panel text, and DIAG speaking markers - same as a live session.
        return delegate.submit(request);
    }
}
